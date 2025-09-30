package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.CobrancaStatus;
import br.com.gestaocondominio.api.domain.enums.ReservaAreaComumStatus;
import br.com.gestaocondominio.api.domain.enums.SolicitacaoManutencaoStatus;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.repository.*;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UnidadeServiceImpl implements UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final CondominioRepository condominioRepository;
    private final MoradorRepository moradorRepository;
    private final FinanceiroCobrancaRepository financeiroCobrancaRepository;
    private final ReservaAreaComumRepository reservaAreaComumRepository;
    private final SolicitacaoManutencaoRepository solicitacaoManutencaoRepository;

    public UnidadeServiceImpl(UnidadeRepository unidadeRepository,
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

    @Override
    public Unidade cadastrarUnidade(UnidadeRequestDTO dto) {
        if (dto.getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a unidade.");
        }
        Condominio condominio = condominioRepository.findById(dto.getConCod())
                .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado com o ID: " + dto.getConCod()));

        if (dto.getUniNumero() == null || dto.getUniNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("Número da unidade não pode ser vazio.");
        }

        unidadeRepository.findByUniNumeroAndCondominio(dto.getUniNumero(), condominio).ifPresent(u -> {
            throw new IllegalArgumentException("Já existe uma unidade com este número para o condomínio informado: " + u.getUniNumero());
        });
        
        Unidade unidade = new Unidade();
        unidade.setCondominio(condominio);
        unidade.setUnidadeTipo(dto.getUnidadeTipo());
        unidade.setUniNumero(dto.getUniNumero());
        unidade.setBloco(dto.getBloco());
        unidade.setAndar(dto.getAndar());
        unidade.setFracaoIdeal(dto.getFracaoIdeal());
        unidade.setAreaPrivada(dto.getAreaPrivada());
        unidade.setObservacao(dto.getObservacao());
        unidade.setUniStatusOcupacao(dto.getUniStatusOcupacao() == null ? UnidadeStatusOcupacao.VAZIA : dto.getUniStatusOcupacao());
        unidade.setUniDtCadastro(LocalDateTime.now());
        unidade.setUniDtAtualizacao(LocalDateTime.now());
        unidade.setUniAtiva(dto.getUniAtiva() != null ? dto.getUniAtiva() : true);
        
        return unidadeRepository.save(unidade);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Unidade> listarTodasUnidades(boolean incluirInativas, String statusOcupacao, String busca) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        List<Unidade> unidadesAutorizadas;

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            unidadesAutorizadas = incluirInativas ? unidadeRepository.findAllWithCondominio() : unidadeRepository.findByUniAtivaWithCondominio(true);
        } else {
            Set<Integer> condoIdsComAcessoAdmin = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_");
            if (!condoIdsComAcessoAdmin.isEmpty()) {
                List<Condominio> condominiosGerenciados = condominioRepository.findAllById(condoIdsComAcessoAdmin);
                unidadesAutorizadas = unidadeRepository.findByCondominioInWithCondominio(condominiosGerenciados);
            } else {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                Pessoa pessoaLogada = userDetails.getPessoa();
                List<Morador> vinculosMorador = moradorRepository.findByPessoa(pessoaLogada);
                unidadesAutorizadas = vinculosMorador.stream().map(Morador::getUnidade).collect(Collectors.toList());
            }

            if (!incluirInativas) {
                unidadesAutorizadas = unidadesAutorizadas.stream().filter(u -> u.getUniAtiva() != null && u.getUniAtiva()).collect(Collectors.toList());
            }
        }

        Stream<Unidade> stream = unidadesAutorizadas.stream();

        if (statusOcupacao != null && !statusOcupacao.isBlank() && !statusOcupacao.equalsIgnoreCase("Todos")) {
            UnidadeStatusOcupacao statusEnum = UnidadeStatusOcupacao.valueOf(statusOcupacao.toUpperCase());
            stream = stream.filter(u -> u.getUniStatusOcupacao() == statusEnum);
        }

        if (busca != null && !busca.isBlank()) {
            String buscaLower = busca.toLowerCase();
            stream = stream.filter(u -> 
                (u.getUniNumero() != null && u.getUniNumero().toLowerCase().contains(buscaLower)) ||
                (u.getBloco() != null && u.getBloco().toLowerCase().contains(buscaLower))
            );
        }

        return stream.collect(Collectors.toList());
    }

    @Override
    public Optional<Unidade> buscarUnidadePorId(Integer id) {
        Optional<Unidade> unidadeOpt = unidadeRepository.findById(id);
        unidadeOpt.ifPresent(this::checkPermissionToViewUnit);
        return unidadeOpt;
    }

    @Override
    public Unidade atualizarUnidade(Integer id, UnidadeRequestDTO dto) {
        Unidade unidadeExistente = unidadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));

        checkAdminOrSindicoPermission(unidadeExistente.getCondominio().getConCod());
        
        if (dto.getUniNumero() != null && !dto.getUniNumero().equalsIgnoreCase(unidadeExistente.getUniNumero())) {
            unidadeRepository.findByUniNumeroAndCondominio(dto.getUniNumero(), unidadeExistente.getCondominio()).ifPresent(u -> {
                if (!u.getUniCod().equals(id))
                    throw new IllegalArgumentException("Novo número de unidade já cadastrado para o condomínio: " + u.getUniNumero());
            });
            unidadeExistente.setUniNumero(dto.getUniNumero());
        }

        unidadeExistente.setUnidadeTipo(dto.getUnidadeTipo());
        unidadeExistente.setUniStatusOcupacao(dto.getUniStatusOcupacao());
        unidadeExistente.setBloco(dto.getBloco());
        unidadeExistente.setAndar(dto.getAndar());
        unidadeExistente.setFracaoIdeal(dto.getFracaoIdeal());
        unidadeExistente.setAreaPrivada(dto.getAreaPrivada());
        unidadeExistente.setObservacao(dto.getObservacao());
        
        if (dto.getUniAtiva() != null) {
            unidadeExistente.setUniAtiva(dto.getUniAtiva());
        }
        
        unidadeExistente.setUniDtAtualizacao(LocalDateTime.now());
        return unidadeRepository.save(unidadeExistente);
    }

    @Override
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

    @Override
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