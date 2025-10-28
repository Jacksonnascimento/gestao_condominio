package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.*;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.*;
import br.com.gestaocondominio.api.exception.StorageException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OcorrenciaServiceImpl implements OcorrenciaService {

    @Autowired
    private OcorrenciaRepository ocorrenciaRepository;
    @Autowired
    private OcorrenciaComentarioRepository comentarioRepository;
    @Autowired
    private OcorrenciaAnexoRepository anexoRepository;
    @Autowired
    private UnidadeRepository unidadeRepository;
    @Autowired
    private CondominioRepository condominioRepository;
    @Autowired
    private OcupanteRepository ocupanteRepository;
    @Autowired
    private UsuarioCondominioService usuarioCondominioService;
    @Autowired
    private FileStorageService fileStorageService;

    private static final String OCORRENCIAS_DIR = "ocorrencias";

    @Override
    @Transactional(readOnly = true)
    public Page<OcorrenciaResumoDTO> consultarOcorrencias(Pessoa usuarioLogado, Integer condominioIdFiltroTela,
            String buscaUnidade, String buscaTitulo, OcorrenciaTipo tipo,
            OcorrenciaStatus status, LocalDate inicioApos, LocalDate fimAntes,
            Pageable pageable) {

        Specification<Ocorrencia> spec = OcorrenciaSpecification.filtrar(
                usuarioLogado, condominioIdFiltroTela, buscaUnidade, buscaTitulo, tipo, status,
                inicioApos, fimAntes, usuarioCondominioService, false);

        Page<Ocorrencia> ocorrenciaPage = ocorrenciaRepository.findAll(spec, pageable);

        List<OcorrenciaResumoDTO> dtos = ocorrenciaPage.getContent().stream()
                .map(OcorrenciaResumoDTO::new)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, ocorrenciaPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> contarOcorrenciasPorStatusEPeriodo(Pessoa usuarioLogado, Integer condominioIdFiltroTela,
            String buscaUnidade,
            String buscaTitulo, OcorrenciaTipo tipo, OcorrenciaStatus status,
            LocalDate inicioApos, LocalDate fimAntes) {

        Specification<Ocorrencia> specBase = OcorrenciaSpecification.filtrar(
                usuarioLogado, condominioIdFiltroTela, buscaUnidade, buscaTitulo, tipo, status,
                inicioApos, fimAntes, usuarioCondominioService, true);

        List<Ocorrencia> ocorrenciasFiltradas = ocorrenciaRepository.findAll(specBase);

        long totalFiltrado = ocorrenciasFiltradas.size();
        long abertas = ocorrenciasFiltradas.stream().filter(o -> o.getStatus() == OcorrenciaStatus.ABERTA).count();
        long emAnalise = ocorrenciasFiltradas.stream().filter(o -> o.getStatus() == OcorrenciaStatus.EM_ANALISE)
                .count();
        long resolvidas = ocorrenciasFiltradas.stream().filter(o -> o.getStatus() == OcorrenciaStatus.RESOLVIDA)
                .count();
        long esteMes = ocorrenciasFiltradas.stream()
                .filter(o -> o.getDataRegistro().isAfter(LocalDateTime.now().minusMonths(1))).count();

        return Map.of(
                "TOTAL", totalFiltrado,
                "ABERTA", abertas,
                "EM_ANALISE", emAnalise,
                "RESOLVIDA", resolvidas,
                "ESTE_MES", esteMes);
    }

    @Override
    @Transactional(readOnly = true)
    public OcorrenciaDetalhesDTO buscarPorIdDetalhes(Integer id, Pessoa usuarioLogado) {
        Ocorrencia ocorrencia = buscarOcorrenciaPorIdEValidarAcesso(id, usuarioLogado, false);
        return OcorrenciaDetalhesDTO.fromEntity(ocorrencia);
    }

    @Override
    @Transactional
    public Ocorrencia criarOcorrencia(OcorrenciaRequestDTO dto, Pessoa usuarioLogado) {
        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada com ID: " + dto.getUnidadeId()));

        Condominio condominio;
        if (Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin())) {
            if (dto.getCondominioId() == null) {
                throw new IllegalArgumentException("Condomínio deve ser selecionado para Administrador Global.");
            }
            condominio = condominioRepository.findById(dto.getCondominioId())
                    .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado com ID: " + dto.getCondominioId()));

            if (!unidade.getCondominio().getConCod().equals(condominio.getConCod())) {
                 throw new IllegalArgumentException("A unidade selecionada não pertence ao condomínio selecionado.");
            }
        } else {
             condominio = unidade.getCondominio();
        }

        validarAcessoUnidade(unidade, usuarioLogado, true);

        Ocorrencia ocorrencia = Ocorrencia.builder()
                .condominio(condominio)
                .unidade(unidade)
                .pessoaRegistro(usuarioLogado)
                .titulo(dto.getTitulo())
                .descricao(dto.getDescricao())
                .tipo(dto.getTipo())
                .build();

        return ocorrenciaRepository.save(ocorrencia);
    }

    @Override
    @Transactional
    public OcorrenciaComentarioDTO adicionarComentario(Integer ocorrenciaId, OcorrenciaComentarioRequestDTO dto,
            Pessoa usuarioLogado) {
        Ocorrencia ocorrencia = buscarOcorrenciaPorIdEValidarAcesso(ocorrenciaId, usuarioLogado, true);

        if (ocorrencia.getStatus() == OcorrenciaStatus.RESOLVIDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível adicionar comentários a ocorrências resolvidas.");
        }

        OcorrenciaComentario comentario = OcorrenciaComentario.builder()
                .ocorrencia(ocorrencia)
                .pessoaComentario(usuarioLogado)
                .comentario(dto.getComentario())
                .build();

        if (ocorrencia.getStatus() == OcorrenciaStatus.ABERTA &&
                (usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado,
                        UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM))) {
            ocorrencia.setStatus(OcorrenciaStatus.EM_ANALISE);
            ocorrenciaRepository.save(ocorrencia);
        }

        OcorrenciaComentario savedComentario = comentarioRepository.save(comentario);
        return new OcorrenciaComentarioDTO(savedComentario);
    }

    @Override
    @Transactional
    public OcorrenciaAnexoDTO adicionarAnexo(Integer ocorrenciaId, MultipartFile anexo, Pessoa usuarioLogado) {
        if (anexo == null || anexo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo inválido ou vazio.");
        }

        Ocorrencia ocorrencia = buscarOcorrenciaPorIdEValidarAcesso(ocorrenciaId, usuarioLogado, true);

        if (ocorrencia.getStatus() == OcorrenciaStatus.RESOLVIDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível adicionar anexos a ocorrências resolvidas.");
        }

        String caminhoRelativo = fileStorageService.store(anexo, OCORRENCIAS_DIR);

        OcorrenciaAnexo ocorrenciaAnexo = OcorrenciaAnexo.builder()
                .ocorrencia(ocorrencia)
                .pessoaAnexo(usuarioLogado)
                .caminhoArquivo(caminhoRelativo)
                .nomeOriginal(anexo.getOriginalFilename())
                .tipoArquivo(anexo.getContentType())
                .tamanhoArquivo(anexo.getSize())
                .build();

        if (ocorrencia.getStatus() == OcorrenciaStatus.ABERTA &&
                (usuarioLogado.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuarioLogado,
                        UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM))) {
            ocorrencia.setStatus(OcorrenciaStatus.EM_ANALISE);
            ocorrenciaRepository.save(ocorrencia);
        }

        OcorrenciaAnexo savedAnexo = anexoRepository.save(ocorrenciaAnexo);
        return new OcorrenciaAnexoDTO(savedAnexo);
    }

    @Override
    @Transactional
    public void excluirAnexo(Integer ocorrenciaId, Integer anexoId, Pessoa usuarioLogado) {
        Ocorrencia ocorrencia = buscarOcorrenciaPorIdEValidarAcesso(ocorrenciaId, usuarioLogado, true);

        if (ocorrencia.getStatus() == OcorrenciaStatus.RESOLVIDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível excluir anexos de ocorrências resolvidas.");
        }

        OcorrenciaAnexo anexo = anexoRepository.findByOcorrenciaOcoCodAndOcaCod(ocorrenciaId, anexoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Anexo não encontrado com ID: " + anexoId + " para a ocorrência ID: " + ocorrenciaId));

        try {
            String simpleFilename = Paths.get(anexo.getCaminhoArquivo()).getFileName().toString();
            fileStorageService.delete(simpleFilename, OCORRENCIAS_DIR);
            anexoRepository.delete(anexo); // <-- CORREÇÃO: Adicionada exclusão do banco
        } catch (StorageException e) {
            throw new RuntimeException("Falha ao excluir o arquivo físico do anexo: " + e.getMessage(), e);
        } catch (Exception e) {
             throw new RuntimeException("Falha ao excluir o registro do anexo no banco de dados: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource carregarAnexoComoRecurso(Integer ocorrenciaId, Integer anexoId, Pessoa usuarioLogado) {
        buscarOcorrenciaPorIdEValidarAcesso(ocorrenciaId, usuarioLogado, false);

        OcorrenciaAnexo anexo = anexoRepository.findByOcorrenciaOcoCodAndOcaCod(ocorrenciaId, anexoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, // <-- MUDANÇA: EntityNotFound -> ResponseStatusException 404
                        "Anexo não encontrado com ID: " + anexoId + " para a ocorrência ID: " + ocorrenciaId));

        try {
            String simpleFilename = Paths.get(anexo.getCaminhoArquivo()).getFileName().toString();
            Resource resource = fileStorageService.loadAsResource(simpleFilename, OCORRENCIAS_DIR);

            // <-- VERIFICAÇÃO ADICIONAL (Embora loadAsResource já lance exceção se não existir)
            if (!resource.exists() || !resource.isReadable()) {
                 throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado ou inacessível no armazenamento.");
            }
            return resource;
        } catch (StorageException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado ou inacessível: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getNomeOriginalAnexo(Integer ocorrenciaId, Integer anexoId, Pessoa usuarioLogado) {
        buscarOcorrenciaPorIdEValidarAcesso(ocorrenciaId, usuarioLogado, false);

        OcorrenciaAnexo anexo = anexoRepository.findByOcorrenciaOcoCodAndOcaCod(ocorrenciaId, anexoId)
                .orElseThrow(() -> new EntityNotFoundException( // Mantém EntityNotFound pois é só busca de nome
                        "Anexo não encontrado com ID: " + anexoId + " para a ocorrência ID: " + ocorrenciaId));

        return anexo.getNomeOriginal() != null ? anexo.getNomeOriginal() : "anexo_" + anexo.getOcaCod();
    }

    @Override
    @Transactional
    public Ocorrencia finalizarOcorrencia(Integer ocorrenciaId, OcorrenciaFinalizarRequestDTO dto,
            Pessoa usuarioLogado) {
        if (!usuarioLogado.getPesIsGlobalAdmin() && !usuarioCondominioService.possuiRole(usuarioLogado,
                UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas usuários com permissão gerencial podem finalizar ocorrências.");
        }

        Ocorrencia ocorrencia = buscarOcorrenciaPorIdEValidarAcesso(ocorrenciaId, usuarioLogado, true);

        if (ocorrencia.getStatus() == OcorrenciaStatus.RESOLVIDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta ocorrência já está resolvida.");
        }

        if (!usuarioLogado.getPesIsGlobalAdmin()) {
            Integer conCodOcorrencia = ocorrencia.getCondominio().getConCod();
            boolean temPermissaoNoCondominio = usuarioCondominioService.findByPessoa(usuarioLogado).stream()
                    .anyMatch(uc -> uc.getConCod().equals(conCodOcorrencia) &&
                            (uc.getUscPapel() == UserRole.SINDICO || uc.getUscPapel() == UserRole.ADMIN
                                    || uc.getUscPapel() == UserRole.FUNCIONARIO_ADM));
            if (!temPermissaoNoCondominio) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você não tem permissão para finalizar ocorrências neste condomínio.");
            }
        }

        ocorrencia.setStatus(OcorrenciaStatus.RESOLVIDA);
        ocorrencia.setParecerFinal(dto.getParecerFinal());
        ocorrencia.setPessoaFinalizou(usuarioLogado);
        ocorrencia.setDataFinalizacao(LocalDateTime.now());
        ocorrencia.setDataAtualizacao(LocalDateTime.now());
        return ocorrenciaRepository.save(ocorrencia);
    }

    @Override
    @Transactional(readOnly = true)
    public Ocorrencia buscarOcorrenciaPorIdEValidarAcesso(Integer id, Pessoa usuarioLogado, boolean edicao) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ocorrência não encontrada com ID: " + id));

        validarAcessoUnidade(ocorrencia.getUnidade(), usuarioLogado, edicao);
        return ocorrencia;
    }

    private void validarAcessoUnidade(Unidade unidade, Pessoa usuarioLogado, boolean edicao) {
        if (usuarioLogado == null || unidade == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso Negado.");
        }

        Integer conCodUnidade = unidade.getCondominio() != null ? unidade.getCondominio().getConCod() : null;
        if (conCodUnidade == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unidade sem condomínio associado.");
        }

        if (Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin())) {
            return;
        }

        boolean isGerencialNoCondominio = usuarioCondominioService.findByPessoa(usuarioLogado).stream()
                .anyMatch(uc -> uc.getConCod().equals(conCodUnidade) &&
                        (uc.getUscPapel() == UserRole.SINDICO || uc.getUscPapel() == UserRole.ADMIN
                                || uc.getUscPapel() == UserRole.FUNCIONARIO_ADM));

        if (isGerencialNoCondominio) {
            return;
        }

        boolean isOcupanteDaUnidade = unidadeRepository.findByIdWithCondominio(unidade.getUniCod())
                .map(u -> u.getCondominio().getConCod().equals(conCodUnidade))
                .orElse(false) &&
                ocupanteRepository.findByPessoa(usuarioLogado).stream()
                        .anyMatch(oc -> oc.getUnidade() != null
                                && oc.getUnidade().getUniCod().equals(unidade.getUniCod()));

        if (isOcupanteDaUnidade) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso Negado. Você não tem permissão para "
                + (edicao ? "modificar" : "visualizar") + " ocorrências desta unidade.");
    }
}