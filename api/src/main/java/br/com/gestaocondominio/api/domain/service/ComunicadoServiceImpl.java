package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ComunicadoRequestDTO;
import br.com.gestaocondominio.api.controller.v1.dto.ComunicadoDTOs.ComunicadoResposta;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.*;
import br.com.gestaocondominio.api.exception.StorageException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComunicadoServiceImpl implements ComunicadoService {

    private final ComunicadoRepository comunicadoRepository;
    private final CondominioRepository condominioRepository;
    private final FileStorageService fileStorageService;
    private final UsuarioCondominioService usuarioCondominioService;
    private final ComunicadoLeituraRepository comunicadoLeituraRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final OcupanteRepository ocupanteRepository;

    private static final String COMUNICADOS_DIR = "comunicados";
    private static final Set<PublicoDestino> TODOS_OS_PUBLICOS = Set.of(PublicoDestino.values());

    public ComunicadoServiceImpl(ComunicadoRepository comunicadoRepository,
                                 CondominioRepository condominioRepository,
                                 FileStorageService fileStorageService,
                                 UsuarioCondominioService usuarioCondominioService,
                                 ComunicadoLeituraRepository comunicadoLeituraRepository,
                                 UsuarioCondominioRepository usuarioCondominioRepository,
                                 OcupanteRepository ocupanteRepository) {
        this.comunicadoRepository = comunicadoRepository;
        this.condominioRepository = condominioRepository;
        this.fileStorageService = fileStorageService;
        this.usuarioCondominioService = usuarioCondominioService;
        this.comunicadoLeituraRepository = comunicadoLeituraRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.ocupanteRepository = ocupanteRepository;
    }

    /**
     * Comunicados que a pessoa pode ver, com os filtros da tela. O administrador geral vê todos; os demais veem os do
     * seu condomínio (o primeiro vínculo), e só o síndico e a administração veem todos os públicos: o morador vê os
     * destinados a todos e ao seu tipo de vínculo com a unidade, e os funcionários, os destinados aos funcionários.
     */
    private Specification<Comunicado> especificacaoVisivel(Pessoa pessoaLogada, String titulo, String mensagem,
                                                           String publicoDestinoFiltroTela, Boolean isUrgente) {
        Integer conCodAtivo = null;
        Set<PublicoDestino> publicosPermitidosParaVisualizar = new HashSet<>();
        boolean isUsuarioAdminCondo = false;

        if (Boolean.TRUE.equals(pessoaLogada.getPesIsGlobalAdmin())) {
            conCodAtivo = null;
            publicosPermitidosParaVisualizar.addAll(TODOS_OS_PUBLICOS);
        } else {
            conCodAtivo = usuarioCondominioService.getCondominioIdDoUsuario(pessoaLogada);
            if (conCodAtivo == null) {
                return (root, query, cb) -> cb.disjunction();
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

        return ComunicadoSpecification.filtrar(
                pessoaLogada,
                conCodAtivo,
                publicosPermitidosParaVisualizar,
                isUsuarioAdminCondo,
                titulo,
                mensagem,
                publicoDestinoFiltroTela,
                isUrgente
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComunicadoResposta> consultarVisiveis(Pessoa usuarioLogado, String titulo, String mensagem,
                                                      PublicoDestino publicoDestino, Boolean urgente,
                                                      Pageable pageable) {
        Integer condominioGerenciado = condominioGerenciado(usuarioLogado);
        boolean administradorGeral = Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin());
        return comunicadoRepository.findAll(especificacaoVisivel(usuarioLogado, titulo, mensagem,
                        publicoDestino == null ? null : publicoDestino.name(), urgente), pageable)
                .map(comunicado -> ComunicadoResposta.de(comunicado, administradorGeral,
                        podeGerenciar(usuarioLogado, condominioGerenciado, comunicado)));
    }

    @Override
    @Transactional(readOnly = true)
    public ComunicadoResposta buscarVisivel(Integer id, Pessoa usuarioLogado) {
        Comunicado comunicado = buscarVisivelOuFalhar(id, usuarioLogado);
        return ComunicadoResposta.de(comunicado, Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin()),
                podeGerenciar(usuarioLogado, condominioGerenciado(usuarioLogado), comunicado));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean podeGerenciar(Pessoa usuarioLogado) {
        return Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin()) || condominioGerenciado(usuarioLogado) != null;
    }

    @Override
    @Transactional
    public ComunicadoResposta criar(ComunicadoRequestDTO dto, MultipartFile anexo, Pessoa usuarioLogado) {
        if (!podeGerenciar(usuarioLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas o síndico ou a administração do condomínio podem publicar comunicados.");
        }
        conferirCondominiosDeDestino(usuarioLogado, dto.getCondominioIds());
        Comunicado comunicado = gravarNovo(dto, anexo, usuarioLogado);
        return ComunicadoResposta.de(comunicado, Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin()), true);
    }

    @Override
    @Transactional
    public ComunicadoResposta atualizar(Integer id, ComunicadoRequestDTO dto, MultipartFile anexo,
                                        Pessoa usuarioLogado) {
        Comunicado comunicado = buscarParaGerenciar(id, usuarioLogado);
        conferirCondominiosDeDestino(usuarioLogado, dto.getCondominioIds());
        Comunicado salvo = gravarAlteracao(comunicado, dto, anexo, usuarioLogado);
        return ComunicadoResposta.de(salvo, Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin()), true);
    }

    @Override
    @Transactional
    public void excluir(Integer id, Pessoa usuarioLogado) {
        apagar(buscarParaGerenciar(id, usuarioLogado));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource carregarAnexo(Integer id, Pessoa usuarioLogado) {
        Comunicado comunicado = buscarVisivelOuFalhar(id, usuarioLogado);
        if (comunicado.getCaminhoAnexo() == null || comunicado.getCaminhoAnexo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Este comunicado não possui anexo.");
        }
        String nomeDoArquivo = Paths.get(comunicado.getCaminhoAnexo()).getFileName().toString();
        try {
            return fileStorageService.loadAsResource(nomeDoArquivo, COMUNICADOS_DIR);
        } catch (StorageException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Arquivo não encontrado no servidor. Pode ter sido excluído ou movido.");
        }
    }

    /** O comunicado, se quem está logado pode vê-lo pela mesma regra da listagem. */
    private Comunicado buscarVisivelOuFalhar(Integer id, Pessoa usuarioLogado) {
        Comunicado comunicado = comunicadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comunicado não encontrado."));
        if (Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin())) {
            return comunicado;
        }
        // Contagem, e não exists(): a especificação só busca as associações junto quando a consulta não é de total.
        Specification<Comunicado> visivel = especificacaoVisivel(usuarioLogado, null, null, null, null);
        Specification<Comunicado> doId = (root, query, cb) -> cb.equal(root.get("comId"), id);
        if (comunicadoRepository.count(visivel.and(doId)) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Acesso negado. Você não tem permissão para ver este comunicado.");
        }
        return comunicado;
    }

    /**
     * O comunicado, se quem está logado pode alterá-lo: o administrador geral altera qualquer um; o síndico e a
     * administração, os publicados só para o condomínio deles. Um comunicado que o administrador geral mandou para
     * vários condomínios só ele altera, porque salvar pela tela do condomínio o tiraria dos outros.
     */
    private Comunicado buscarParaGerenciar(Integer id, Pessoa usuarioLogado) {
        Comunicado comunicado = comunicadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comunicado não encontrado."));
        if (!podeGerenciar(usuarioLogado, condominioGerenciado(usuarioLogado), comunicado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Acesso negado. Você não tem permissão para alterar este comunicado.");
        }
        return comunicado;
    }

    private boolean podeGerenciar(Pessoa usuarioLogado, Integer condominioGerenciado, Comunicado comunicado) {
        if (Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin())) {
            return true;
        }
        if (condominioGerenciado == null || comunicado.getCondominios() == null) {
            return false;
        }
        Set<Integer> destinos = comunicado.getCondominios().stream()
                .map(Condominio::getConCod)
                .collect(Collectors.toSet());
        return destinos.equals(Set.of(condominioGerenciado));
    }

    /**
     * Condomínio em que a pessoa publica comunicados: o mesmo que a listagem usa (o primeiro vínculo), desde que nele
     * ela seja síndico ou administração, com o vínculo ativo. Nulo quando não publica em nenhum.
     */
    private Integer condominioGerenciado(Pessoa usuarioLogado) {
        if (Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin())) {
            return null;
        }
        Integer conCodAtivo = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
        if (conCodAtivo == null) {
            return null;
        }
        boolean gerencia = usuarioCondominioRepository.findByPesCod(usuarioLogado.getPesCod()).stream()
                .anyMatch(uc -> uc.getConCod().equals(conCodAtivo)
                        && Boolean.TRUE.equals(uc.getUscAtivoAssociacao())
                        && (uc.getUscPapel() == UserRole.ADMIN || uc.getUscPapel() == UserRole.SINDICO));
        return gerencia ? conCodAtivo : null;
    }

    /** O administrador geral escolhe os condomínios; todos precisam existir. Os demais publicam no próprio. */
    private void conferirCondominiosDeDestino(Pessoa usuarioLogado, List<Integer> condominioIds) {
        if (!Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin())) {
            return;
        }
        if (condominioIds == null || condominioIds.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um condomínio.");
        }
        Set<Integer> pedidos = new HashSet<>(condominioIds);
        if (pedidos.contains(null) || condominioRepository.findAllById(pedidos).size() != pedidos.size()) {
            throw new IllegalArgumentException("Um dos condomínios selecionados não foi encontrado.");
        }
    }

    // ---- Gravação ----

    private Comunicado gravarNovo(ComunicadoRequestDTO dto, MultipartFile anexo, Pessoa criador) {
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

            return comunicadoRepository.save(comunicado);

        } catch (RuntimeException e) {
            if (caminhoAnexo != null) {
                String simpleFilename = Paths.get(caminhoAnexo).getFileName().toString();
                fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
            }
            throw e;
        }
    }

    /** Um anexo novo substitui o antigo, que é apagado depois de salvar. */
    private Comunicado gravarAlteracao(Comunicado comunicado, ComunicadoRequestDTO dto, MultipartFile anexo,
                                       Pessoa editor) {
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

            Comunicado comunicadoSalvo = comunicadoRepository.save(comunicado);

            if (novoCaminhoAnexo != null && antigoCaminhoAnexo != null) {
                String simpleFilename = Paths.get(antigoCaminhoAnexo).getFileName().toString();
                fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
            }

            return comunicadoSalvo;

        } catch (RuntimeException e) {
            if (novoCaminhoAnexo != null && !novoCaminhoAnexo.equals(antigoCaminhoAnexo)) {
                String simpleFilename = Paths.get(novoCaminhoAnexo).getFileName().toString();
                fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
            }
            throw e;
        }
    }

    private void apagar(Comunicado comunicado) {
        String caminhoAnexo = comunicado.getCaminhoAnexo();

        comunicadoLeituraRepository.deleteByComunicadoId(comunicado.getComId());
        comunicado.getCondominios().clear();
        comunicadoRepository.save(comunicado);
        comunicadoRepository.delete(comunicado);

        if (caminhoAnexo != null) {
            String simpleFilename = Paths.get(caminhoAnexo).getFileName().toString();
            fileStorageService.delete(simpleFilename, COMUNICADOS_DIR);
        }
    }

    private Set<Condominio> getCondominiosAlvo(Pessoa pessoa, List<Integer> condominioIds) {
        Set<Condominio> condominiosAlvo = new HashSet<>();

        if (Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin())) {
            if (condominioIds == null || condominioIds.isEmpty()) {
                throw new IllegalArgumentException("Selecione ao menos um condomínio.");
            }
            condominiosAlvo.addAll(condominioRepository.findAllById(condominioIds));
        } else {
            Integer conCodAtivo = usuarioCondominioService.getCondominioIdDoUsuario(pessoa);
            if (conCodAtivo == null) {
                throw new IllegalArgumentException("Você não está vinculado a nenhum condomínio.");
            }
            Condominio condominioAtivo = condominioRepository.findById(conCodAtivo)
                    .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado."));
            condominiosAlvo.add(condominioAtivo);
        }

        if (condominiosAlvo.isEmpty()) {
            throw new IllegalArgumentException("Nenhum dos condomínios selecionados foi encontrado.");
        }
        return condominiosAlvo;
    }
}
