package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ComunicadoRequestDTO;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComunicadoServiceImpl implements ComunicadoService {

    private final ComunicadoRepository comunicadoRepository;
    private final CondominioRepository condominioRepository;
    private final PessoaService pessoaService;
    private final FileStorageService fileStorageService;
    private final UsuarioCondominioService usuarioCondominioService;
    private final ComunicadoLeituraRepository comunicadoLeituraRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final OcupanteRepository ocupanteRepository;

    private static final String COMUNICADOS_DIR = "comunicados";
    private static final Set<PublicoDestino> TODOS_OS_PUBLICOS = Set.of(PublicoDestino.values());

    public ComunicadoServiceImpl(ComunicadoRepository comunicadoRepository,
                                 CondominioRepository condominioRepository,
                                 PessoaService pessoaService,
                                 FileStorageService fileStorageService,
                                 UsuarioCondominioService usuarioCondominioService,
                                 ComunicadoLeituraRepository comunicadoLeituraRepository,
                                 UsuarioCondominioRepository usuarioCondominioRepository,
                                 OcupanteRepository ocupanteRepository) {
        this.comunicadoRepository = comunicadoRepository;
        this.condominioRepository = condominioRepository;
        this.pessoaService = pessoaService;
        this.fileStorageService = fileStorageService;
        this.usuarioCondominioService = usuarioCondominioService;
        this.comunicadoLeituraRepository = comunicadoLeituraRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.ocupanteRepository = ocupanteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comunicado> consultar(
            String titulo,
            String mensagem,
            String publicoDestinoFiltroTela,
            Boolean isUrgente,
            Pageable pageable) {

        Pessoa pessoaLogada = pessoaService.getLoggedInUser();
        Integer conCodAtivo = null;
        Set<PublicoDestino> publicosPermitidosParaVisualizar = new HashSet<>();
        boolean isUsuarioAdminCondo = false;

        if (Boolean.TRUE.equals(pessoaLogada.getPesIsGlobalAdmin())) {
            conCodAtivo = null;
            publicosPermitidosParaVisualizar.addAll(TODOS_OS_PUBLICOS);
        } else {
            conCodAtivo = usuarioCondominioService.getCondominioIdDoUsuario(pessoaLogada);
            if (conCodAtivo == null) {
                Specification<Comunicado> spec = (root, query, cb) -> cb.disjunction();
                return comunicadoRepository.findAll(spec, pageable);
            }

            final Integer finalConCodAtivo = conCodAtivo;

            Set<UserRole> roles = usuarioCondominioRepository.findByPesCod(pessoaLogada.getPesCod())
                    .stream()
                    .filter(uc -> uc.getConCod().equals(finalConCodAtivo))
                    .map(UsuarioCondominio::getUscPapel)
                    .collect(Collectors.toSet());

            isUsuarioAdminCondo = roles.contains(UserRole.ADMIN) || roles.contains(UserRole.SINDICO);

            if (isUsuarioAdminCondo) {
                publicosPermitidosParaVisualizar.addAll(TODOS_OS_PUBLICOS);
            } else {
                publicosPermitidosParaVisualizar.add(PublicoDestino.TODOS);

                if (roles.contains(UserRole.FUNCIONARIO_ADM) || roles.contains(UserRole.PORTEIRO)) {
                    publicosPermitidosParaVisualizar.add(PublicoDestino.FUNCIONARIOS);
                }

                Set<OcupanteVinculo> vinculos = ocupanteRepository.findByPessoa(pessoaLogada)
                        .stream()
                        .filter(oc -> oc.getUnidade() != null && oc.getUnidade().getCondominio() != null && oc.getUnidade().getCondominio().getConCod().equals(finalConCodAtivo))
                        .map(Ocupante::getOcuVinculo)
                        .collect(Collectors.toSet());

                if (vinculos.contains(OcupanteVinculo.PROPRIETARIO)) {
                    publicosPermitidosParaVisualizar.add(PublicoDestino.PROPRIETARIOS);
                }
                if (vinculos.contains(OcupanteVinculo.LOCATARIO)) {
                    publicosPermitidosParaVisualizar.add(PublicoDestino.INQUILINOS);
                }
            }
        }

        Specification<Comunicado> spec = ComunicadoSpecification.filtrar(
                pessoaLogada,
                conCodAtivo,
                publicosPermitidosParaVisualizar,
                isUsuarioAdminCondo,
                titulo,
                mensagem,
                publicoDestinoFiltroTela,
                isUrgente
        );

        return comunicadoRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Comunicado getComunicadoById(Integer id) {
        return comunicadoRepository.findById(id)
                .map(comunicado -> {
                    comunicado.getCondominios().size();
                    return comunicado;
                })
                .orElseThrow(() -> new EntityNotFoundException("Comunicado não encontrado. ID: " + id));
    }

    @Override
    @Transactional
    public void criar(ComunicadoRequestDTO dto, MultipartFile anexo) {
        Pessoa criador = pessoaService.getLoggedInUser();
        String caminhoAnexo = null;

        try {
            if (anexo != null) {
                if (anexo.isEmpty()) {
                    throw new IllegalArgumentException("Não é possível anexar um arquivo vazio.");
                }
                caminhoAnexo = fileStorageService.store(anexo, COMUNICADOS_DIR);
            }

            Set<Condominio> condominiosAlvo = getCondominiosAlvo(criador, dto.getCondominioIds());

            Comunicado comunicado = Comunicado.builder()
                    .titulo(dto.getTitulo())
                    .mensagem(dto.getMensagem())
                    .publicoDestino(dto.getPublicoDestino())
                    .isUrgente(dto.getIsUrgente())
                    .caminhoAnexo(caminhoAnexo)
                    .criador(criador)
                    .condominios(condominiosAlvo)
                    .build();

            comunicadoRepository.save(comunicado);

        } catch (Exception e) {
            if (caminhoAnexo != null) {
                String simpleFilename = Paths.get(caminhoAnexo).getFileName().toString();
                fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
            }
            throw new RuntimeException("Falha ao criar comunicado: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void atualizar(Integer id, ComunicadoRequestDTO dto, MultipartFile anexo) {
        Pessoa editor = pessoaService.getLoggedInUser();
        Comunicado comunicado = comunicadoRepository.findById(id)
                .map(c -> {
                    c.getCondominios().size();
                    return c;
                })
                .orElseThrow(() -> new EntityNotFoundException("Comunicado não encontrado para atualização. ID: " + id));

        String novoCaminhoAnexo = null;
        String antigoCaminhoAnexo = comunicado.getCaminhoAnexo();

        try {
            if (anexo != null) {
                if (anexo.isEmpty()) {
                    throw new IllegalArgumentException("Não é possível anexar um arquivo vazio.");
                }
                novoCaminhoAnexo = fileStorageService.store(anexo, COMUNICADOS_DIR);
                comunicado.setCaminhoAnexo(novoCaminhoAnexo);
            }

            Set<Condominio> condominiosAlvo = getCondominiosAlvo(editor, dto.getCondominioIds());

            comunicado.setTitulo(dto.getTitulo());
            comunicado.setMensagem(dto.getMensagem());
            comunicado.setPublicoDestino(dto.getPublicoDestino());
            comunicado.setIsUrgente(dto.getIsUrgente());
            comunicado.setCondominios(condominiosAlvo);

            comunicadoRepository.save(comunicado);

            if (novoCaminhoAnexo != null && antigoCaminhoAnexo != null) {
                String simpleFilename = Paths.get(antigoCaminhoAnexo).getFileName().toString();
                fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
            }

        } catch (Exception e) {
            if (novoCaminhoAnexo != null && !novoCaminhoAnexo.equals(antigoCaminhoAnexo)) {
                String simpleFilename = Paths.get(novoCaminhoAnexo).getFileName().toString();
                fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
            }
            throw new RuntimeException("Falha ao atualizar comunicado: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void excluir(Integer id) {
        Comunicado comunicado = comunicadoRepository.findById(id)
                .map(c -> {
                    c.getCondominios().size();
                    return c;
                })
                .orElseThrow(() -> new EntityNotFoundException("Comunicado não encontrado para exclusão. ID: " + id));

        String caminhoAnexo = comunicado.getCaminhoAnexo();

        try {
            comunicadoLeituraRepository.deleteByComunicadoId(id);
            comunicado.getCondominios().clear();
            comunicadoRepository.save(comunicado);
            comunicadoRepository.delete(comunicado);

            if (caminhoAnexo != null) {
                String simpleFilename = Paths.get(caminhoAnexo).getFileName().toString();
                fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao excluir comunicado ID " + id + ": " + e.getMessage(), e);
        }
    }

    private Set<Condominio> getCondominiosAlvo(Pessoa pessoa, List<Integer> condominioIds) {
        Set<Condominio> condominiosAlvo = new HashSet<>();

        if (Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin())) {
            if (condominioIds == null || condominioIds.isEmpty()) {
                throw new IllegalArgumentException("Admin Global deve selecionar ao menos um condomínio.");
            }
            condominiosAlvo.addAll(condominioRepository.findAllById(condominioIds));
        } else {
            Integer conCodAtivo = usuarioCondominioService.getCondominioIdDoUsuario(pessoa);
            if (conCodAtivo == null) {
                throw new EntityNotFoundException("Usuário não possui um condomínio ativo na sessão.");
            }
            Condominio condominioAtivo = condominioRepository.findById(conCodAtivo)
                    .orElseThrow(() -> new EntityNotFoundException("Condomínio ativo não encontrado. ID: " + conCodAtivo));
            condominiosAlvo.add(condominioAtivo);
        }

        if (condominiosAlvo.isEmpty()) {
            throw new EntityNotFoundException("Nenhum condomínio de destino foi definido ou encontrado.");
        }
        return condominiosAlvo;
    }
}