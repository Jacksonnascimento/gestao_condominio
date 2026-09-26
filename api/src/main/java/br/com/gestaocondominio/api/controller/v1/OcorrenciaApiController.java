package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.dto.OcorrenciaComentarioDTO;
import br.com.gestaocondominio.api.controller.dto.OcorrenciaComentarioRequestDTO;
import br.com.gestaocondominio.api.controller.dto.OcorrenciaFinalizarRequestDTO;
import br.com.gestaocondominio.api.controller.dto.OcorrenciaResumoDTO;
import br.com.gestaocondominio.api.controller.v1.dto.OcorrenciaDTOs.Anexo;
import br.com.gestaocondominio.api.controller.v1.dto.OcorrenciaDTOs.CondominioResumo;
import br.com.gestaocondominio.api.controller.v1.dto.OcorrenciaDTOs.OcorrenciaDetalhe;
import br.com.gestaocondominio.api.controller.v1.dto.OcorrenciaDTOs.OcorrenciaRequest;
import br.com.gestaocondominio.api.controller.v1.dto.OcorrenciaDTOs.UnidadeResumo;
import br.com.gestaocondominio.api.controller.v1.dto.Opcao;
import br.com.gestaocondominio.api.controller.v1.dto.Pagina;
import br.com.gestaocondominio.api.domain.entity.Ocorrencia;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.OcorrenciaService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Ocorrências registradas pelos moradores e tratadas pela gestão do condomínio. Quem vê cada ocorrência é decidido
 * pelo {@link OcorrenciaService}: o morador vê as que registrou e as da sua unidade; síndico, administração e
 * funcionário administrativo veem as do condomínio. Como na tela, comentários, anexos e a finalização ficam só com
 * quem gerencia o condomínio da ocorrência.
 */
@RestController
@RequestMapping("/api/v1/ocorrencias")
@Tag(name = "Ocorrências")
public class OcorrenciaApiController {

    private final OcorrenciaService ocorrenciaService;
    private final PessoaService pessoaService;
    private final CondominioService condominioService;
    private final UnidadeService unidadeService;
    private final UsuarioCondominioService usuarioCondominioService;

    public OcorrenciaApiController(OcorrenciaService ocorrenciaService, PessoaService pessoaService,
                                   CondominioService condominioService, UnidadeService unidadeService,
                                   UsuarioCondominioService usuarioCondominioService) {
        this.ocorrenciaService = ocorrenciaService;
        this.pessoaService = pessoaService;
        this.condominioService = condominioService;
        this.unidadeService = unidadeService;
        this.usuarioCondominioService = usuarioCondominioService;
    }

    @GetMapping
    @Operation(summary = "Lista as ocorrências visíveis para quem está logado: abertas, em análise e resolvidas, "
            + "e dentro de cada situação as mais recentes primeiro")
    public Pagina<OcorrenciaResumoDTO> listar(@RequestParam(required = false) Integer condominioId,
                                              @RequestParam(required = false) String buscaUnidade,
                                              @RequestParam(required = false) String buscaTitulo,
                                              @RequestParam(required = false) OcorrenciaTipo tipo,
                                              @RequestParam(required = false) OcorrenciaStatus status,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicioApos,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fimAntes,
                                              @RequestParam(defaultValue = "0") int pagina,
                                              @RequestParam(defaultValue = "20") int tamanho) {
        // A ordem vem da consulta do serviço.
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100));
        return Pagina.de(ocorrenciaService.consultarOcorrencias(usuarioLogado(), condominioId, buscaUnidade,
                buscaTitulo, tipo, status, inicioApos, fimAntes, pageable));
    }

    @GetMapping("/totais")
    @Operation(summary = "Quantidade de ocorrências por situação, do último mês (ESTE_MES) e no total (TOTAL), com "
            + "os mesmos filtros da listagem")
    public Map<String, Long> totais(@RequestParam(required = false) Integer condominioId,
                                    @RequestParam(required = false) String buscaUnidade,
                                    @RequestParam(required = false) String buscaTitulo,
                                    @RequestParam(required = false) OcorrenciaTipo tipo,
                                    @RequestParam(required = false) OcorrenciaStatus status,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicioApos,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fimAntes) {
        return ocorrenciaService.contarOcorrenciasPorStatusEPeriodo(usuarioLogado(), condominioId, buscaUnidade,
                buscaTitulo, tipo, status, inicioApos, fimAntes);
    }

    /**
     * As unidades são as que podem receber uma ocorrência nova: as ativas do condomínio de quem está logado, ou,
     * para o administrador geral, as do {@code condominioId} escolhido no formulário.
     */
    @GetMapping("/opcoes")
    @Operation(summary = "Tipos, situações, condomínios e unidades para os formulários, e se quem está logado "
            + "gerencia ocorrências")
    public OpcoesOcorrencia opcoes(@RequestParam(required = false) Integer condominioId) {
        Pessoa usuario = usuarioLogado();
        Integer condominioDasUnidades = Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin())
                ? condominioId
                : usuarioCondominioService.getCondominioIdDoUsuario(usuario);
        List<Unidade> unidades = condominioDasUnidades == null
                ? List.of()
                : unidadeService.findAtivasByCondominioId(condominioDasUnidades);
        return new OpcoesOcorrencia(
                Opcao.de(OcorrenciaTipo.class, OcorrenciaTipo::getDescricao),
                Opcao.de(OcorrenciaStatus.class, OcorrenciaStatus::getDescricao),
                ocorrenciaService.podeGerenciarOcorrencias(usuario),
                condominioService.listarTodosCondominios(false).stream().map(CondominioResumo::de).toList(),
                unidades.stream().map(UnidadeResumo::de).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Uma ocorrência, com comentários e anexos para quem a gerencia")
    public OcorrenciaDetalhe buscar(@PathVariable Integer id) {
        return detalhe(id, usuarioLogado());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma ocorrência para uma unidade do condomínio")
    public OcorrenciaDetalhe registrar(@Valid @RequestBody OcorrenciaRequest pedido) {
        Pessoa usuario = usuarioLogado();
        Ocorrencia ocorrencia = ocorrenciaService.criarOcorrencia(pedido.paraDTO(), usuario);
        return detalhe(ocorrencia.getOcoCod(), usuario);
    }

    /** O primeiro comentário da gestão numa ocorrência aberta a coloca em análise. */
    @PostMapping("/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comenta a ocorrência")
    public OcorrenciaComentarioDTO comentar(@PathVariable Integer id,
                                            @Valid @RequestBody OcorrenciaComentarioRequestDTO pedido) {
        Pessoa usuario = usuarioLogado();
        ocorrenciaService.conferirGestao(id, usuario);
        return ocorrenciaService.adicionarComentario(id, pedido, usuario);
    }

    /** O limite de tamanho do arquivo é o do envio de arquivos do sistema (10 MB), como na tela. */
    @PostMapping(value = "/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Anexa um arquivo à ocorrência", description = "Formulário multipart com o arquivo na "
            + "parte `anexo`. Como o comentário, o primeiro anexo da gestão numa ocorrência aberta a coloca em análise.")
    public Anexo anexar(@PathVariable Integer id, @RequestPart("anexo") MultipartFile anexo) {
        Pessoa usuario = usuarioLogado();
        ocorrenciaService.conferirGestao(id, usuario);
        return Anexo.de(ocorrenciaService.adicionarAnexo(id, anexo, usuario));
    }

    @DeleteMapping("/{id}/anexos/{anexoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um anexo de uma ocorrência ainda não resolvida")
    public void excluirAnexo(@PathVariable Integer id, @PathVariable Integer anexoId) {
        Pessoa usuario = usuarioLogado();
        ocorrenciaService.conferirGestao(id, usuario);
        ocorrenciaService.excluirAnexo(id, anexoId, usuario);
    }

    /** O arquivo sai sempre como download, para o navegador não abrir como página um HTML enviado como anexo. */
    @GetMapping("/{id}/anexos/{anexoId}")
    @Operation(summary = "Baixa um anexo da ocorrência")
    public ResponseEntity<Resource> baixarAnexo(@PathVariable Integer id, @PathVariable Integer anexoId) {
        Pessoa usuario = usuarioLogado();
        ocorrenciaService.conferirGestao(id, usuario);
        Resource arquivo = ocorrenciaService.carregarAnexoComoRecurso(id, anexoId, usuario);
        String nome = ocorrenciaService.getNomeOriginalAnexo(id, anexoId, usuario);
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(nome).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.CONTENT_DISPOSITION, ComunicadoApiController.comoDownload(nome))
                .header("X-Content-Type-Options", "nosniff")
                .body(arquivo);
    }

    @PostMapping("/{id}/finalizacao")
    @Operation(summary = "Finaliza a ocorrência com o parecer final, marcando-a como resolvida")
    public OcorrenciaDetalhe finalizar(@PathVariable Integer id,
                                       @Valid @RequestBody OcorrenciaFinalizarRequestDTO pedido) {
        Pessoa usuario = usuarioLogado();
        ocorrenciaService.conferirGestao(id, usuario);
        ocorrenciaService.finalizarOcorrencia(id, pedido, usuario);
        return detalhe(id, usuario);
    }

    private OcorrenciaDetalhe detalhe(Integer id, Pessoa usuario) {
        return OcorrenciaDetalhe.de(ocorrenciaService.buscarPorIdDetalhes(id, usuario),
                ocorrenciaService.gerenciaOcorrencia(id, usuario));
    }

    private Pessoa usuarioLogado() {
        return pessoaService.getLoggedInUser();
    }

    public record OpcoesOcorrencia(List<Opcao> tipos, List<Opcao> status, boolean podeGerenciar,
                                   List<CondominioResumo> condominios, List<UnidadeResumo> unidades) {
    }
}
