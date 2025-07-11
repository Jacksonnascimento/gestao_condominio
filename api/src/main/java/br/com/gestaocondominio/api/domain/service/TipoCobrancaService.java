package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import br.com.gestaocondominio.api.domain.enums.CobrancaStatus;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.FinanceiroCobrancaRepository;
import br.com.gestaocondominio.api.domain.repository.TipoCobrancaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TipoCobrancaService {

    private final TipoCobrancaRepository tipoCobrancaRepository;
    private final FinanceiroCobrancaRepository financeiroCobrancaRepository;
    private final CondominioRepository condominioRepository;

    public TipoCobrancaService(TipoCobrancaRepository tipoCobrancaRepository,
                               FinanceiroCobrancaRepository financeiroCobrancaRepository,
                               CondominioRepository condominioRepository) {
        this.tipoCobrancaRepository = tipoCobrancaRepository;
        this.financeiroCobrancaRepository = financeiroCobrancaRepository;
        this.condominioRepository = condominioRepository;
    }

    public TipoCobranca cadastrarTipoCobranca(TipoCobranca tipoCobranca) {
        if (tipoCobranca.getTicDescricao() == null || tipoCobranca.getTicDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição do tipo de cobrança não pode ser vazia.");
        }
        
        Condominio condominio = condominioRepository.findById(tipoCobranca.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado."));
        tipoCobranca.setCondominio(condominio);

        Optional<TipoCobranca> tipoExistente = tipoCobrancaRepository.findByTicDescricaoAndCondominio(tipoCobranca.getTicDescricao(), condominio);
        if (tipoExistente.isPresent()) {
            throw new IllegalArgumentException("Já existe um tipo de cobrança com esta descrição neste condomínio: " + tipoCobranca.getTicDescricao());
        }

        tipoCobranca.setTicDtCadastro(LocalDateTime.now());
        tipoCobranca.setTicDtAtualizacao(LocalDateTime.now());

        if (tipoCobranca.getTicAtiva() == null) {
            tipoCobranca.setTicAtiva(true);
        }

        return tipoCobrancaRepository.save(tipoCobranca);
    }

    public Optional<TipoCobranca> buscarTipoCobrancaPorId(Integer id) {
        Optional<TipoCobranca> tipoCobrancaOpt = tipoCobrancaRepository.findById(id);
        tipoCobrancaOpt.ifPresent(this::checkPermissionToView);
        return tipoCobrancaOpt;
    }

    public List<TipoCobranca> listarTodosTiposCobranca(boolean incluirInativas) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return incluirInativas ? tipoCobrancaRepository.findAll() : tipoCobrancaRepository.findByTicAtiva(true);
        }
        
        Set<Integer> condoIds = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_", "ROLE_MORADOR_");
        if(condoIds.isEmpty()){
            return List.of();
        }

        List<Condominio> condominios = condominioRepository.findAllById(condoIds);
        
        Stream<TipoCobranca> streamDeTipos = condominios.stream()
                .flatMap(condo -> {
                    if (incluirInativas) {
                        return tipoCobrancaRepository.findByCondominio(condo).stream();
                    } else {
                        return tipoCobrancaRepository.findByCondominioAndTicAtiva(condo, true).stream();
                    }
                });

        return streamDeTipos.distinct().collect(Collectors.toList());
    }

    public TipoCobranca atualizarTipoCobranca(Integer id, TipoCobranca tipoCobrancaAtualizada) {
        TipoCobranca tipoCobrancaExistente = tipoCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cobrança não encontrado com o ID: " + id));

        checkPermissionToManage(tipoCobrancaExistente);

        if (tipoCobrancaAtualizada.getTicDescricao() == null || tipoCobrancaAtualizada.getTicDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição do tipo de cobrança não pode ser vazia na atualização.");
        }

        if (!tipoCobrancaExistente.getTicDescricao().equalsIgnoreCase(tipoCobrancaAtualizada.getTicDescricao())) {
            Optional<TipoCobranca> tipoConflito = tipoCobrancaRepository.findByTicDescricaoAndCondominio(tipoCobrancaAtualizada.getTicDescricao(), tipoCobrancaExistente.getCondominio());
            if (tipoConflito.isPresent() && !tipoConflito.get().getTicCod().equals(id)) {
                throw new IllegalArgumentException("Nova descrição já cadastrada para outro tipo de cobrança neste condomínio: " + tipoCobrancaAtualizada.getTicDescricao());
            }
        }

        tipoCobrancaExistente.setTicDescricao(tipoCobrancaAtualizada.getTicDescricao());
        
        if (tipoCobrancaAtualizada.getTicAtiva() != null) {
            tipoCobrancaExistente.setTicAtiva(tipoCobrancaAtualizada.getTicAtiva());
        }

        if (tipoCobrancaAtualizada.getTicValor() != null) {
            tipoCobrancaExistente.setTicValor(tipoCobrancaAtualizada.getTicValor());
        }

        tipoCobrancaExistente.setTicDtAtualizacao(LocalDateTime.now());
        return tipoCobrancaRepository.save(tipoCobrancaExistente);
    }

    public TipoCobranca inativarTipoCobranca(Integer id) {
        TipoCobranca tipo = tipoCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cobrança não encontrado com o ID: " + id));
        
        checkPermissionToManage(tipo);

        List<br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca> cobrancasAtivas = 
            financeiroCobrancaRepository.findByTipoCobrancaAndFicStatusPagamentoNotIn(
                tipo, 
                Arrays.asList(CobrancaStatus.PAGA, CobrancaStatus.CANCELADA) 
            );

        if (!cobrancasAtivas.isEmpty()) {
            throw new IllegalArgumentException("Não é possível inativar o tipo de cobrança, pois existem cobranças financeiras ATIVAS ou PENDENTES vinculadas a ele.");
        }

        tipo.setTicAtiva(false);
        tipo.setTicDtAtualizacao(LocalDateTime.now());
        return tipoCobrancaRepository.save(tipo);
    }

    public TipoCobranca ativarTipoCobranca(Integer id) {
        TipoCobranca tipo = tipoCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cobrança não encontrado com o ID: " + id));
        
        checkPermissionToManage(tipo);
        
        tipo.setTicAtiva(true);
        tipo.setTicDtAtualizacao(LocalDateTime.now());
        return tipoCobrancaRepository.save(tipo);
    }

    private void checkPermissionToView(TipoCobranca tipoCobranca) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) return;

        Integer condominioId = tipoCobranca.getCondominio().getConCod();
        boolean hasAccess = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_", "ROLE_MORADOR_")
                            .contains(condominioId);
        
        if (!hasAccess) {
            throw new AccessDeniedException("Acesso negado para visualizar este tipo de cobrança.");
        }
    }

    private void checkPermissionToManage(TipoCobranca tipoCobranca) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) return;

        Integer condominioId = tipoCobranca.getCondominio().getConCod();
        boolean hasAccess = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_")
                            .contains(condominioId);
        
        if (!hasAccess) {
            throw new AccessDeniedException("Acesso negado para gerenciar este tipo de cobrança.");
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