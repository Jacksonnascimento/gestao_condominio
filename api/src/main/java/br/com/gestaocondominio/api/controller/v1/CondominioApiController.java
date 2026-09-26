package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.v1.dto.CondominioDTOs.CondominioRequest;
import br.com.gestaocondominio.api.controller.v1.dto.CondominioDTOs.CondominioResposta;
import br.com.gestaocondominio.api.controller.v1.dto.CondominioDTOs.OpcoesCondominio;
import br.com.gestaocondominio.api.controller.v1.dto.Opcao;
import br.com.gestaocondominio.api.controller.v1.dto.UnidadeDTOs.UnidadeResumo;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.enums.CondominioTipologia;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Condomínios do cliente. Cada pessoa vê os condomínios em que tem vínculo ativo (o administrador geral vê todos);
 * cadastrar, editar, ativar e inativar é só do administrador geral. As unidades ativas de um condomínio alimentam as
 * listas de escolha dos formulários dos outros módulos.
 */
@RestController
@RequestMapping("/api/v1/condominios")
@Tag(name = "Condomínios")
public class CondominioApiController {

    private static final String ADMINISTRADOR_GERAL = "hasAuthority('ROLE_GLOBAL_ADMIN')";

    private final CondominioService condominioService;

    public CondominioApiController(CondominioService condominioService) {
        this.condominioService = condominioService;
    }

    @GetMapping
    @Operation(summary = "Lista, em ordem alfabética, os condomínios que quem está logado pode ver")
    public List<CondominioResposta> listar(@RequestParam(defaultValue = "false") boolean incluirInativos) {
        return condominioService.listarTodosCondominios(incluirInativos).stream()
                .sorted(Comparator.comparing(Condominio::getConNome, String.CASE_INSENSITIVE_ORDER))
                .map(CondominioResposta::de)
                .toList();
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Tipologias para o formulário, e se quem está logado pode cadastrar condomínios")
    public OpcoesCondominio opcoes() {
        return new OpcoesCondominio(
                Opcao.de(CondominioTipologia.class, t -> t.name().charAt(0) + t.name().substring(1).toLowerCase()),
                condominioService.podeGerenciarCondominios());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Um condomínio")
    public CondominioResposta buscar(@PathVariable Integer id) {
        return CondominioResposta.de(condominioService.buscarCondominioVisivel(id));
    }

    @GetMapping("/{id}/unidades")
    @Operation(summary = "Unidades ativas do condomínio, para as listas de escolha dos formulários")
    public List<UnidadeResumo> unidades(@PathVariable Integer id) {
        return condominioService.listarUnidadesAtivas(id).stream().map(UnidadeResumo::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMINISTRADOR_GERAL)
    @Operation(summary = "Cadastra um condomínio (administrador geral)")
    public CondominioResposta cadastrar(@Valid @RequestBody CondominioRequest pedido) {
        return CondominioResposta.de(condominioService.cadastrarCondominio(pedido.paraEntidade()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ADMINISTRADOR_GERAL)
    @Operation(summary = "Altera os dados do condomínio (administrador geral)")
    public CondominioResposta atualizar(@PathVariable Integer id, @Valid @RequestBody CondominioRequest pedido) {
        condominioService.buscarCondominioVisivel(id);
        return CondominioResposta.de(condominioService.atualizarCondominio(id, pedido.paraEntidade()));
    }

    /** Só inativa condomínio sem unidades e sem usuários vinculados; senão responde 409 dizendo o motivo. */
    @PutMapping("/{id}/inativar")
    @PreAuthorize(ADMINISTRADOR_GERAL)
    @Operation(summary = "Inativa o condomínio (administrador geral)")
    public CondominioResposta inativar(@PathVariable Integer id) {
        condominioService.buscarCondominioVisivel(id);
        return CondominioResposta.de(condominioService.inativarCondominio(id));
    }

    @PutMapping("/{id}/ativar")
    @PreAuthorize(ADMINISTRADOR_GERAL)
    @Operation(summary = "Reativa o condomínio (administrador geral)")
    public CondominioResposta ativar(@PathVariable Integer id) {
        condominioService.buscarCondominioVisivel(id);
        return CondominioResposta.de(condominioService.ativarCondominio(id));
    }
}
