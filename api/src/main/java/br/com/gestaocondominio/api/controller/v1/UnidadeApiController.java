package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.v1.dto.Opcao;
import br.com.gestaocondominio.api.controller.v1.dto.Pagina;
import br.com.gestaocondominio.api.controller.v1.dto.UnidadeDTOs.OpcoesUnidade;
import br.com.gestaocondominio.api.controller.v1.dto.UnidadeDTOs.UnidadeInativaResposta;
import br.com.gestaocondominio.api.controller.v1.dto.UnidadeDTOs.UnidadeRequest;
import br.com.gestaocondominio.api.controller.v1.dto.UnidadeDTOs.UnidadeResposta;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.enums.UnidadeTipo;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import br.com.gestaocondominio.api.exception.UnidadeInativaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Unidades dos condomínios. Quem vê e quem altera cada unidade é decidido pelo {@link UnidadeService}: o
 * administrador geral vê todas; síndico, administradora e funcionário administrativo veem as do condomínio; o morador
 * vê as que ocupa. Cadastrar, editar, inativar e reativar é do síndico e da administradora do condomínio.
 */
@RestController
@RequestMapping("/api/v1/unidades")
@Tag(name = "Unidades")
public class UnidadeApiController {

    private final UnidadeService unidadeService;
    private final PessoaService pessoaService;

    public UnidadeApiController(UnidadeService unidadeService, PessoaService pessoaService) {
        this.unidadeService = unidadeService;
        this.pessoaService = pessoaService;
    }

    @GetMapping
    @Operation(summary = "Lista as unidades visíveis para quem está logado, por número")
    public Pagina<UnidadeResposta> listar(@RequestParam(required = false) Integer condominioId,
                                          @RequestParam(required = false) String busca,
                                          @RequestParam(required = false) UnidadeStatusOcupacao status,
                                          @RequestParam(defaultValue = "false") boolean incluirInativas,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "20") int tamanho) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100),
                Sort.by("uniNumero", "bloco", "uniCod"));
        return Pagina.de(unidadeService.consultarUnidades(usuarioLogado(), condominioId, busca, status,
                incluirInativas, pageable), UnidadeResposta::de);
    }

    @GetMapping("/totais")
    @Operation(summary = "Quantidade de unidades por situação de ocupação, com os mesmos filtros da listagem")
    public Map<String, Long> totais(@RequestParam(required = false) Integer condominioId,
                                    @RequestParam(required = false) String busca,
                                    @RequestParam(required = false) UnidadeStatusOcupacao status,
                                    @RequestParam(defaultValue = "false") boolean incluirInativas) {
        return unidadeService.contarUnidadesPorStatus(usuarioLogado(), condominioId, busca, status, incluirInativas);
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Tipos e situações de ocupação para os formulários, e se quem está logado pode cadastrar unidades")
    public OpcoesUnidade opcoes() {
        return new OpcoesUnidade(
                Opcao.de(UnidadeTipo.class, UnidadeTipo::getDescricao),
                Opcao.de(UnidadeStatusOcupacao.class, UnidadeStatusOcupacao::getDescricao),
                unidadeService.podeGerenciarUnidades());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Uma unidade")
    public UnidadeResposta buscar(@PathVariable Integer id) {
        return UnidadeResposta.de(unidadeService.buscarUnidadeVisivel(id, usuarioLogado()));
    }

    /**
     * Se a mesma unidade (número, bloco e tipo) já existiu e foi inativada, responde 409 com o {@code unidadeId} dela,
     * para a tela oferecer a reativação em vez de criar outra.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma unidade")
    public UnidadeResposta cadastrar(@Valid @RequestBody UnidadeRequest pedido) {
        Unidade unidade = unidadeService.cadastrarUnidade(pedido.paraDto());
        return UnidadeResposta.de(unidadeService.buscarUnidadeVisivel(unidade.getUniCod(), usuarioLogado()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera os dados da unidade")
    public UnidadeResposta atualizar(@PathVariable Integer id, @Valid @RequestBody UnidadeRequest pedido) {
        Pessoa usuario = usuarioLogado();
        unidadeService.buscarUnidadeVisivel(id, usuario);
        unidadeService.atualizarUnidade(id, pedido.paraDto());
        return UnidadeResposta.de(unidadeService.buscarUnidadeVisivel(id, usuario));
    }

    /** Só inativa unidade sem ocupantes; é a "exclusão" da tela. */
    @PutMapping("/{id}/inativar")
    @Operation(summary = "Inativa a unidade")
    public UnidadeResposta inativar(@PathVariable Integer id) {
        Pessoa usuario = usuarioLogado();
        unidadeService.buscarUnidadeVisivel(id, usuario);
        unidadeService.inativarUnidade(id);
        return UnidadeResposta.de(unidadeService.buscarUnidadeVisivel(id, usuario));
    }

    @PutMapping("/{id}/reativar")
    @Operation(summary = "Reativa a unidade")
    public UnidadeResposta reativar(@PathVariable Integer id) {
        Pessoa usuario = usuarioLogado();
        unidadeService.buscarUnidadeVisivel(id, usuario);
        unidadeService.ativarUnidade(id);
        return UnidadeResposta.de(unidadeService.buscarUnidadeVisivel(id, usuario));
    }

    @ExceptionHandler(UnidadeInativaException.class)
    public ResponseEntity<UnidadeInativaResposta> unidadeInativa(UnidadeInativaException ex,
                                                                 HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new UnidadeInativaResposta(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(),
                "Esta unidade já existiu e foi inativada. Reative o cadastro antigo em vez de criar outro.",
                request.getRequestURI(), ex.getUnidadeId()));
    }

    private Pessoa usuarioLogado() {
        return pessoaService.getLoggedInUser();
    }
}
