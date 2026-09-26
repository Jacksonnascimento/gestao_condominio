package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeSpecification;
import br.com.gestaocondominio.api.exception.ConflitoException;
import br.com.gestaocondominio.api.exception.UnidadeInativaException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service("unidadeServiceImpl")
public class UnidadeServiceImpl implements UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final CondominioRepository condominioRepository;
    private final OcupanteRepository ocupanteRepository;

    public UnidadeServiceImpl(UnidadeRepository unidadeRepository,
            CondominioRepository condominioRepository,
            OcupanteRepository ocupanteRepository) {
        this.unidadeRepository = unidadeRepository;
        this.condominioRepository = condominioRepository;
        this.ocupanteRepository = ocupanteRepository;

    }

    @Override
    @Transactional
    public Unidade cadastrarUnidade(UnidadeRequestDTO dto) {
        Integer condominioId = dto.getConCod();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (condominioId == null) {
            Set<Integer> condoIdsComAcesso = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_");

            if (condoIdsComAcesso.size() == 1) {
                condominioId = condoIdsComAcesso.iterator().next();
                dto.setConCod(condominioId);
            } else {
                throw new IllegalArgumentException("Escolha o condomínio.");
            }
        }

        checkAdminOrSindicoPermissionForCondominio(condominioId);

        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado."));

        if (dto.getUniNumero() == null || dto.getUniNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("Informe o número da unidade.");
        }

        // VERIFICAÇÃO DE UNIDADE EXISTENTE (ATIVA OU INATIVA)
        Optional<Unidade> unidadeExistente = unidadeRepository.findByCondominioAndUniNumeroAndBlocoAndUnidadeTipo(
                condominio, dto.getUniNumero(), dto.getBloco(), dto.getUnidadeTipo());

        if (unidadeExistente.isPresent()) {
            Unidade u = unidadeExistente.get();
            if (Boolean.FALSE.equals(u.getUniAtiva())) {
                // Exceção própria: a resposta leva o código da unidade, para a tela oferecer a reativação
                throw new UnidadeInativaException(u.getUniCod());
            } else {
                throw new ConflitoException("Já existe uma unidade ativa com este número, bloco e tipo neste condomínio.");
            }
        }

        Unidade unidade = new Unidade();
        unidade.setCondominio(condominio);
        unidade.setUnidadeTipo(dto.getUnidadeTipo());
        unidade.setUniNumero(dto.getUniNumero());
        unidade.setBloco(dto.getBloco());
        unidade.setAndar(dto.getAndar());
        unidade.setFracaoIdeal(dto.getFracaoIdeal());
        unidade.setAreaPrivada(dto.getAreaPrivada());
        unidade.setObservacao(dto.getObservacao());
        unidade.setUniStatusOcupacao(
                dto.getUniStatusOcupacao() == null ? UnidadeStatusOcupacao.VAZIA : dto.getUniStatusOcupacao());
        unidade.setUniDtCadastro(LocalDateTime.now());
        unidade.setUniDtAtualizacao(LocalDateTime.now());
        unidade.setUniAtiva(dto.getUniAtiva() != null ? dto.getUniAtiva() : true);

        return unidadeRepository.save(unidade);
    }

    @Override
    @Transactional
    public Unidade atualizarUnidade(Integer id, UnidadeRequestDTO dto) {
        Unidade unidadeExistente = unidadeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));

        checkAdminOrSindicoPermissionForCondominio(unidadeExistente.getCondominio().getConCod());

        unidadeRepository.findByCondominioAndUniNumeroAndBlocoAndUnidadeTipo(
                unidadeExistente.getCondominio(), dto.getUniNumero(), dto.getBloco(), dto.getUnidadeTipo())
                .ifPresent(u -> {
                    if (!u.getUniCod().equals(id)) {
                        throw new ConflitoException(
                                "Já existe uma unidade com este número, bloco e tipo neste condomínio.");
                    }
                });

        unidadeExistente.setUniNumero(dto.getUniNumero());
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
    @Transactional
    public Unidade inativarUnidade(Integer id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));

        checkAdminOrSindicoPermissionForCondominio(unidade.getCondominio().getConCod());

        if (!ocupanteRepository.findByUnidade(unidade).isEmpty()) {
            throw new ConflitoException(
                    "Não é possível inativar a unidade, pois existem ocupantes vinculados a ela.");
        }

        unidade.setUniAtiva(false);
        unidade.setUniDtAtualizacao(LocalDateTime.now());
        return unidadeRepository.save(unidade);
    }

    @Override
    @Transactional
    public Unidade ativarUnidade(Integer id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));

        checkAdminOrSindicoPermissionForCondominio(unidade.getCondominio().getConCod());

        unidade.setUniAtiva(true);
        unidade.setUniDtAtualizacao(LocalDateTime.now());
        return unidadeRepository.save(unidade);
    }

    private void checkAdminOrSindicoPermissionForCondominio(Integer condominioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean hasPermission = hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
                hasAuthority(authentication, "ROLE_SINDICO_" + condominioId) ||
                hasAuthority(authentication, "ROLE_ADMIN_" + condominioId);

        if (!hasPermission) {
            throw new AccessDeniedException(
                    "Acesso negado. Você não tem permissão para gerenciar unidades neste condomínio.");
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

    @Override
    @Transactional(readOnly = true)
    public List<Unidade> findAtivasByCondominioId(Integer condominioId) {
        return unidadeRepository.findAtivasByCondominioConCodWithCondominio(condominioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Unidade> consultarUnidades(Pessoa usuario, Integer condominioId, String busca,
                                           UnidadeStatusOcupacao status, boolean incluirInativas, Pageable pageable) {
        return unidadeRepository.findAll(
                especificacaoVisivel(usuario, condominioId, busca, status, incluirInativas), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> contarUnidadesPorStatus(Pessoa usuario, Integer condominioId, String busca,
                                                     UnidadeStatusOcupacao status, boolean incluirInativas) {
        List<Unidade> unidades = unidadeRepository.findAll(
                especificacaoVisivel(usuario, condominioId, busca, status, incluirInativas));
        Map<String, Long> totais = new LinkedHashMap<>();
        totais.put("TOTAL", (long) unidades.size());
        for (UnidadeStatusOcupacao situacao : UnidadeStatusOcupacao.values()) {
            totais.put(situacao.name(), unidades.stream().filter(u -> u.getUniStatusOcupacao() == situacao).count());
        }
        return totais;
    }

    @Override
    @Transactional(readOnly = true)
    public Unidade buscarUnidadeVisivel(Integer id, Pessoa usuario) {
        Unidade unidade = unidadeRepository.findByIdWithCondominio(id)
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));
        if (AcessoPorCondominio.temPapelNoCondominio(unidade.getCondominio().getConCod(), AcessoPorCondominio.GESTAO)
                || ocupanteRepository.findByPessoaAndUnidade(usuario, unidade).isPresent()) {
            return unidade;
        }
        throw new AccessDeniedException("Acesso negado. Você não tem permissão para visualizar esta unidade.");
    }

    @Override
    public boolean podeGerenciarUnidades() {
        return AcessoPorCondominio.temPapelEmAlgumCondominio(AcessoPorCondominio.SINDICO_OU_ADMINISTRADORA);
    }

    private Specification<Unidade> especificacaoVisivel(Pessoa usuario, Integer condominioId, String busca,
                                                        UnidadeStatusOcupacao status, boolean incluirInativas) {
        Specification<Unidade> filtros = UnidadeSpecification.comFiltros(condominioId, busca, status, incluirInativas);
        if (AcessoPorCondominio.administradorGeral()) {
            return filtros;
        }
        Set<Integer> unidadesOcupadas = ocupanteRepository.findByPessoa(usuario).stream()
                .map(o -> o.getUnidade().getUniCod())
                .collect(Collectors.toSet());
        return filtros.and(UnidadeSpecification.visiveis(
                AcessoPorCondominio.condominiosComPapel(AcessoPorCondominio.GESTAO), unidadesOcupadas));
    }
}