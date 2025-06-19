package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Morador;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.CobrancaStatus;
import br.com.gestaocondominio.api.domain.enums.ReservaAreaComumStatus;
import br.com.gestaocondominio.api.domain.enums.SolicitacaoManutencaoStatus;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.repository.*;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final CondominioRepository condominioRepository;
    private final MoradorRepository moradorRepository;
    private final FinanceiroCobrancaRepository financeiroCobrancaRepository;
    private final ReservaAreaComumRepository reservaAreaComumRepository;
    private final SolicitacaoManutencaoRepository solicitacaoManutencaoRepository;

    public UnidadeService(UnidadeRepository unidadeRepository,
                          CondominioRepository condominioRepository,
                          MoradorRepository moradorRepository,
                          FinanceiroCobrancaRepository financeiroCobrancaRepository,
                          ReservaAreaComumRepository reservaAreaComumRepository,
                          SolicitacaoManutencaoRepository solicitacaoManutencaoRepository) {
        this.unidadeRepository = unidadeRepository;
        this.condominioRepository = condominioRepository;
        this.moradorRepository = moradorRepository;
        this.financeiroCobrancaRepository = financeiroCobrancaRepository;
        this.reservaAreaComumRepository = reservaAreaComumRepository;
        this.solicitacaoManutencaoRepository = solicitacaoManutencaoRepository;
    }

    public Unidade cadastrarUnidade(Unidade unidade) {
        if (unidade.getCondominio() == null || unidade.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a unidade.");
        }
        Condominio condominio = condominioRepository.findById(unidade.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + unidade.getCondominio().getConCod()));

        if (unidade.getUniNumero() == null || unidade.getUniNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("Número da unidade não pode ser vazio.");
        }

        unidadeRepository.findByUniNumeroAndCondominio(unidade.getUniNumero(), condominio).ifPresent(u -> {
            throw new IllegalArgumentException("Já existe uma unidade com este número para o condomínio informado: " + u.getUniNumero());
        });

        if (unidade.getUniStatusOcupacao() == null) {
            unidade.setUniStatusOcupacao(UnidadeStatusOcupacao.DESOCUPADO);
        }
        if (unidade.getUniValorTaxaCondominio() == null) {
            unidade.setUniValorTaxaCondominio(BigDecimal.ZERO);
        }
        unidade.setUniDtCadastro(LocalDateTime.now());
        unidade.setUniDtAtualizacao(LocalDateTime.now());
        if (unidade.getUniAtiva() == null) {
            unidade.setUniAtiva(true);
        }
        return unidadeRepository.save(unidade);
    }

    public List<Unidade> listarTodasUnidades(boolean incluirInativas) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Pessoa pessoaLogada = userDetails.getPessoa();

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return incluirInativas ? unidadeRepository.findAll() : unidadeRepository.findByUniAtiva(true);
        }

        Set<Integer> condoIdsComAcessoAdmin = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_");
        if (!condoIdsComAcessoAdmin.isEmpty()) {
            List<Condominio> condominiosGerenciados = condominioRepository.findAllById(condoIdsComAcessoAdmin);
            List<Unidade> unidades = unidadeRepository.findByCondominioIn(condominiosGerenciados);
            if (!incluirInativas) {
                return unidades.stream().filter(u -> u.getUniAtiva() != null && u.getUniAtiva()).collect(Collectors.toList());
            }
            return unidades;
        }

        List<Morador> vinculosMorador = moradorRepository.findByPessoa(pessoaLogada);
        List<Unidade> unidadesDoMorador = vinculosMorador.stream().map(Morador::getUnidade).collect(Collectors.toList());
        if (!incluirInativas) {
            return unidadesDoMorador.stream().filter(u -> u.getUniAtiva() != null && u.getUniAtiva()).collect(Collectors.toList());
        }
        return unidadesDoMorador;
    }

    public Optional<Unidade> buscarUnidadePorId(Integer id) {
        Optional<Unidade> unidadeOpt = unidadeRepository.findById(id);
        unidadeOpt.ifPresent(this::checkPermissionToViewUnit);
        return unidadeOpt;
    }

    public Unidade atualizarUnidade(Integer id, Unidade unidadeAtualizada) {
        Unidade unidadeExistente = unidadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));

        checkAdminOrSindicoPermission(unidadeExistente.getCondominio().getConCod());
        
        if (unidadeAtualizada.getUniNumero() != null && !unidadeAtualizada.getUniNumero().equalsIgnoreCase(unidadeExistente.getUniNumero())) {
            unidadeRepository.findByUniNumeroAndCondominio(unidadeAtualizada.getUniNumero(), unidadeExistente.getCondominio()).ifPresent(u -> {
                if (!u.getUniCod().equals(id))
                    throw new IllegalArgumentException("Novo número de unidade já cadastrado para o condomínio: " + u.getUniNumero());
            });
            unidadeExistente.setUniNumero(unidadeAtualizada.getUniNumero());
        }
        if (unidadeAtualizada.getUniStatusOcupacao() != null) {
            unidadeExistente.setUniStatusOcupacao(unidadeAtualizada.getUniStatusOcupacao());
        }
        if (unidadeAtualizada.getUniValorTaxaCondominio() != null) {
            unidadeExistente.setUniValorTaxaCondominio(unidadeAtualizada.getUniValorTaxaCondominio());
        }
        if (unidadeAtualizada.getUniAtiva() != null) {
            unidadeExistente.setUniAtiva(unidadeAtualizada.getUniAtiva());
        }
        unidadeExistente.setUniDtAtualizacao(LocalDateTime.now());
        return unidadeRepository.save(unidadeExistente);
    }

    public Unidade inativarUnidade(Integer id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));
        
        checkAdminOrSindicoPermission(unidade.getCondominio().getConCod());

        if (!moradorRepository.findByUnidade(unidade).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar a unidade, pois existem moradores vinculados a ela.");
        }
        if (!financeiroCobrancaRepository.findByUnidadeAndFicStatusPagamentoNotIn(unidade, Arrays.asList(CobrancaStatus.PAGA, CobrancaStatus.CANCELADA)).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar a unidade, pois existem cobranças financeiras ativas ou pendentes vinculadas a ela.");
        }
        if (!reservaAreaComumRepository.findByUnidadeAndStatusNotIn(unidade, Arrays.asList(ReservaAreaComumStatus.REALIZADA, ReservaAreaComumStatus.CANCELADA)).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar a unidade, pois existem reservas de áreas comuns ativas ou futuras vinculadas a ela.");
        }
        if (!solicitacaoManutencaoRepository.findByUnidadeAndStatusNotIn(unidade, Arrays.asList(SolicitacaoManutencaoStatus.CONCLUIDA, SolicitacaoManutencaoStatus.CANCELADA)).isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar a unidade, pois existem solicitações de manutenção ativas ou pendentes vinculadas a ela.");
        }
        
        unidade.setUniAtiva(false);
        unidade.setUniDtAtualizacao(LocalDateTime.now());
        return unidadeRepository.save(unidade);
    }

    public Unidade ativarUnidade(Integer id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));
        
        checkAdminOrSindicoPermission(unidade.getCondominio().getConCod());

        unidade.setUniAtiva(true);
        unidade.setUniDtAtualizacao(LocalDateTime.now());
        return unidadeRepository.save(unidade);
    }

    private void checkPermissionToViewUnit(Unidade unidade) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
            hasAuthority(authentication, "ROLE_SINDICO_" + unidade.getCondominio().getConCod()) ||
            hasAuthority(authentication, "ROLE_ADMIN_" + unidade.getCondominio().getConCod())) {
            return;
        }
        
        boolean isMoradorDaUnidade = moradorRepository.findByPessoaAndUnidade(userDetails.getPessoa(), unidade).isPresent();
        if (isMoradorDaUnidade) {
            return;
        }

        throw new AccessDeniedException("Acesso negado. Você não tem permissão para visualizar esta unidade.");
    }

    private void checkAdminOrSindicoPermission(Integer condominioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        boolean hasPermission = hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
                                hasAuthority(authentication, "ROLE_SINDICO_" + condominioId) ||
                                hasAuthority(authentication, "ROLE_ADMIN_" + condominioId);

        if (!hasPermission) {
            throw new AccessDeniedException("Acesso negado. Você não tem permissão para gerenciar unidades neste condomínio.");
        }
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    private Set<Integer> getCondoIdsFromRoles(Authentication auth, String... prefixes) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authString -> Arrays.stream(prefixes).anyMatch(authString::startsWith))
                .map(authString -> Integer.parseInt(authString.substring(authString.lastIndexOf('_') + 1)))
                .collect(Collectors.toSet());
    }
}