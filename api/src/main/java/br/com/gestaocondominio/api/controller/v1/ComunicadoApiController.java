package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.v1.dto.ComunicadoDTOs.ComunicadoRequest;
import br.com.gestaocondominio.api.controller.v1.dto.ComunicadoDTOs.ComunicadoResposta;
import br.com.gestaocondominio.api.controller.v1.dto.ComunicadoDTOs.CondominioResumo;
import br.com.gestaocondominio.api.controller.v1.dto.Opcao;
import br.com.gestaocondominio.api.controller.v1.dto.Pagina;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.service.ComunicadoService;
import br.com.gestaocondominio.api.domain.service.ComunicadoService.AnexoDoComunicado;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

/**
 * Comunicados do condomínio. Quem vê e quem altera cada comunicado é decidido pelo {@link ComunicadoService}, a partir
 * de quem está logado: o morador vê os destinados a todos e ao seu vínculo com a unidade; síndico e administração
 * veem todos os do condomínio e publicam; o administrador geral publica para os condomínios que escolher.
 */
@RestController
@RequestMapping("/api/v1/comunicados")
@Tag(name = "Comunicados")
public class ComunicadoApiController {

    private final ComunicadoService comunicadoService;
    private final PessoaService pessoaService;
    private final CondominioRepository condominioRepository;

    public ComunicadoApiController(ComunicadoService comunicadoService, PessoaService pessoaService,
                                   CondominioRepository condominioRepository) {
        this.comunicadoService = comunicadoService;
        this.pessoaService = pessoaService;
        this.condominioRepository = condominioRepository;
    }

    @GetMapping
    @Operation(summary = "Lista os comunicados que quem está logado vê no condomínio, dos mais recentes para os mais "
            + "antigos", description = "Sem condominioId, vale um dos condomínios de quem está logado; para o "
            + "administrador geral, os de todos os condomínios.")
    public Pagina<ComunicadoResposta> listar(@RequestParam(required = false) Integer condominioId,
                                             @RequestParam(required = false) String titulo,
                                             @RequestParam(required = false) String mensagem,
                                             @RequestParam(required = false) PublicoDestino publicoDestino,
                                             @RequestParam(required = false) Boolean urgente,
                                             @RequestParam(defaultValue = "0") int pagina,
                                             @RequestParam(defaultValue = "10") int tamanho) {
        // A ordem (mais recentes primeiro) vem da consulta do serviço.
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100));
        return Pagina.de(comunicadoService.consultarVisiveis(usuarioLogado(), condominioId, titulo, mensagem,
                publicoDestino, urgente, pageable));
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Públicos para os formulários, se quem está logado pode publicar no condomínio e, para o "
            + "administrador geral, os condomínios de destino")
    public OpcoesComunicado opcoes(@RequestParam(required = false) Integer condominioId) {
        Pessoa usuario = usuarioLogado();
        List<CondominioResumo> condominios = Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin())
                ? condominioRepository.findAll().stream()
                        .map(CondominioResumo::de)
                        .sorted(Comparator.comparing(CondominioResumo::nome,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                        .toList()
                : List.of();
        return new OpcoesComunicado(Opcao.de(PublicoDestino.class, PublicoDestino::getDescricao),
                comunicadoService.podeGerenciar(usuario, condominioId), condominios);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Um comunicado")
    public ComunicadoResposta buscar(@PathVariable Integer id) {
        return comunicadoService.buscarVisivel(id, usuarioLogado());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publica um comunicado",
            description = "Formulário multipart: a parte `comunicado` em JSON (Content-Type application/json) e, "
                    + "opcionalmente, o arquivo na parte `anexo`. Síndico e administração publicam no condomínio "
                    + "do parâmetro condominioId; o administrador geral, nos condominioIds do pedido.")
    public ComunicadoResposta criar(@RequestParam(required = false) Integer condominioId,
                                    @Valid @RequestPart("comunicado") ComunicadoRequest pedido,
                                    @RequestPart(value = "anexo", required = false) MultipartFile anexo) {
        return comunicadoService.criar(pedido.paraDTO(), anexo, usuarioLogado(), condominioId);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Altera um comunicado",
            description = "Mesmo formulário da publicação. Um arquivo novo em `anexo` substitui o anterior; sem "
                    + "arquivo, o anexo atual é mantido.")
    public ComunicadoResposta atualizar(@PathVariable Integer id,
                                        @Valid @RequestPart("comunicado") ComunicadoRequest pedido,
                                        @RequestPart(value = "anexo", required = false) MultipartFile anexo) {
        return comunicadoService.atualizar(id, pedido.paraDTO(), anexo, usuarioLogado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um comunicado e o seu anexo")
    public void excluir(@PathVariable Integer id) {
        comunicadoService.excluir(id, usuarioLogado());
    }

    /**
     * O arquivo sai sempre como download, com o nome com que foi enviado, para o navegador não abrir como página um
     * HTML enviado como anexo.
     */
    @GetMapping("/{id}/anexo")
    @Operation(summary = "Baixa o anexo do comunicado")
    public ResponseEntity<Resource> baixarAnexo(@PathVariable Integer id) {
        AnexoDoComunicado anexo = comunicadoService.carregarAnexo(id, usuarioLogado());
        Resource arquivo = anexo.arquivo();
        String nome = anexo.nome() == null || anexo.nome().isBlank() ? "anexo" : anexo.nome();
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(nome).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.CONTENT_DISPOSITION, comoDownload(nome))
                .header("X-Content-Type-Options", "nosniff")
                .body(arquivo);
    }

    /** Nome simples vai como está; com acento ou símbolo, vai codificado em UTF-8. */
    static String comoDownload(String nome) {
        ContentDisposition.Builder disposicao = ContentDisposition.attachment();
        return (nome.matches("[\\x20-\\x7E&&[^\"\\\\]]+")
                ? disposicao.filename(nome)
                : disposicao.filename(nome, StandardCharsets.UTF_8)).build().toString();
    }

    private Pessoa usuarioLogado() {
        return pessoaService.getLoggedInUser();
    }

    public record OpcoesComunicado(List<Opcao> publicos, boolean podeGerenciar, List<CondominioResumo> condominios) {
    }
}
