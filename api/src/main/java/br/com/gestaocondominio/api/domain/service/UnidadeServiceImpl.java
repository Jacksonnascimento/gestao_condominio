package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @Transactional(readOnly = true)
    public List<Unidade> findByCondominioId(Integer condominioId) {
        checkAdminOrSindicoPermissionForCondominio(condominioId);
        return unidadeRepository.findAtivasByCondominioConCodWithCondominio(condominioId);
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
                throw new IllegalArgumentException("Condomínio deve ser informado para a unidade.");
            }
        }

        checkAdminOrSindicoPermissionForCondominio(condominioId);

        final Integer finalCondominioId = condominioId;
        Condominio condominio = condominioRepository.findById(finalCondominioId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Condomínio não encontrado com o ID: " + finalCondominioId));

        if (dto.getUniNumero() == null || dto.getUniNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("Número da unidade não pode ser vazio.");
        }

        // VERIFICAÇÃO DE UNIDADE EXISTENTE (ATIVA OU INATIVA)
        Optional<Unidade> unidadeExistente = unidadeRepository.findByCondominioAndUniNumeroAndBlocoAndUnidadeTipo(
                condominio, dto.getUniNumero(), dto.getBloco(), dto.getUnidadeTipo());

        if (unidadeExistente.isPresent()) {
            Unidade u = unidadeExistente.get();
            if (Boolean.FALSE.equals(u.getUniAtiva())) {
                // Lança exceção específica que será capturada pelo Controller para oferecer reativação
                throw new IllegalStateException("UNIDADE_INATIVA:" + u.getUniCod());
            } else {
                throw new IllegalArgumentException("Já existe uma unidade ativa com este número, bloco e tipo neste condomínio.");
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
    @Transactional(readOnly = true)
    public List<Unidade> listarTodasUnidades(boolean incluirInativas, String statusOcupacao, String busca) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<Unidade> unidadesAutorizadas;

        if (userDetails.getPessoa().getPesIsGlobalAdmin()) {
            unidadesAutorizadas = incluirInativas ? unidadeRepository.findAllWithCondominio()
                    : unidadeRepository.findByUniAtivaWithCondominio(true);
        } else {
            Set<Integer> condoIdsComAcessoAdmin = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_",
                    "ROLE_FUNCIONARIO_ADM");
            if (!condoIdsComAcessoAdmin.isEmpty()) {
                List<Condominio> condominiosGerenciados = condominioRepository.findAllById(condoIdsComAcessoAdmin);
                unidadesAutorizadas = unidadeRepository.findByCondominioInWithCondominio(condominiosGerenciados);
            } else { // Assume MORADOR ou outro papel restrito
                List<Ocupante> vinculosOcupante = ocupanteRepository.findByPessoa(userDetails.getPessoa());
                unidadesAutorizadas = vinculosOcupante.stream()
                        .map(Ocupante::getUnidade)
                        .collect(Collectors.toList());
            }

            if (!incluirInativas) {
                unidadesAutorizadas = unidadesAutorizadas.stream()
                        .filter(u -> u.getUniAtiva() != null && u.getUniAtiva())
                        .collect(Collectors.toList());
            }
        }

        Stream<Unidade> stream = unidadesAutorizadas.stream();

        if (statusOcupacao != null && !statusOcupacao.isBlank() && !statusOcupacao.equalsIgnoreCase("Todos")) {
            UnidadeStatusOcupacao statusEnum = UnidadeStatusOcupacao.valueOf(statusOcupacao.toUpperCase());
            stream = stream.filter(u -> u.getUniStatusOcupacao() == statusEnum);
        }

        if (busca != null && !busca.isBlank()) {
            String buscaLower = busca.toLowerCase();
            stream = stream
                    .filter(u -> (u.getUniNumero() != null && u.getUniNumero().toLowerCase().contains(buscaLower)) ||
                            (u.getBloco() != null && u.getBloco().toLowerCase().contains(buscaLower)));
        }

        return stream.sorted(Comparator.comparing(Unidade::getUniNumero))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Unidade> buscarUnidadePorId(Integer id) {
        Optional<Unidade> unidadeOpt = unidadeRepository.findByIdWithCondominio(id);
        unidadeOpt.ifPresent(this::checkPermissionToViewUnit);
        return unidadeOpt;
    }

    @Override
    @Transactional
    public Unidade atualizarUnidade(Integer id, UnidadeRequestDTO dto) {
        Unidade unidadeExistente = unidadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));

        checkAdminOrSindicoPermissionForCondominio(unidadeExistente.getCondominio().getConCod());

        unidadeRepository.findByCondominioAndUniNumeroAndBlocoAndUnidadeTipo(
                unidadeExistente.getCondominio(), dto.getUniNumero(), dto.getBloco(), dto.getUnidadeTipo())
                .ifPresent(u -> {
                    if (!u.getUniCod().equals(id)) {
                        throw new IllegalArgumentException(
                                "Já existe uma unidade com este número, bloco e tipo para o condomínio informado.");
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
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));

        checkAdminOrSindicoPermissionForCondominio(unidade.getCondominio().getConCod());

        if (!ocupanteRepository.findByUnidade(unidade).isEmpty()) {
            throw new IllegalArgumentException(
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
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + id));

        checkAdminOrSindicoPermissionForCondominio(unidade.getCondominio().getConCod());

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

        boolean isMoradorDaUnidade = ocupanteRepository.findByPessoaAndUnidade(userDetails.getPessoa(), unidade)
                .isPresent();
        if (isMoradorDaUnidade) {
            return;
        }

        throw new AccessDeniedException("Acesso negado. Você não tem permissão para visualizar esta unidade.");
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

    @Deprecated
    public void checkAdminOrSindicoPermission(Integer unidadeId) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada"));
        checkAdminOrSindicoPermissionForCondominio(unidade.getCondominio().getConCod());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Unidade> findAtivasByCondominioId(Integer condominioId) {
        return unidadeRepository.findAtivasByCondominioConCodWithCondominio(condominioId);
    }
}