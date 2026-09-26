package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.v1.dto.AreaComumDTOs.AreaComumPedido;
import br.com.gestaocondominio.api.controller.v1.dto.AreaComumDTOs.AreaComumResposta;
import br.com.gestaocondominio.api.controller.v1.dto.AreaComumDTOs.CondominioOpcao;
import br.com.gestaocondominio.api.controller.v1.dto.Pagina;
import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.service.AreaComumService;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Cadastro das áreas comuns (salão, churrasqueira...) e dos turnos em que podem ser reservadas. Como na tela antiga,
 * é trabalho de quem gerencia o condomínio: administrador geral, síndico, administração e funcionário adm. O
 * {@link AreaComumService} confere se quem está logado gerencia o condomínio de cada área. Quem vai reservar vê as
 * áreas disponíveis em {@code GET /api/v1/reservas/opcoes}.
 */
@RestController
@RequestMapping("/api/v1/areas-comuns")
@Tag(name = "Áreas comuns")
public class AreaComumApiController {

    private final AreaComumService areaComumService;
    private final CondominioService condominioService;
    private final PessoaService pessoaService;

    public AreaComumApiController(AreaComumService areaComumService, CondominioService condominioService,
                                  PessoaService pessoaService) {
        this.areaComumService = areaComumService;
        this.condominioService = condominioService;
        this.pessoaService = pessoaService;
    }

    @GetMapping
    @Operation(summary = "Lista as áreas comuns que quem está logado gerencia, em ordem de nome")
    public Pagina<AreaComumResposta> listar(@RequestParam(required = false) Integer condominioId,
                                            @RequestParam(required = false) String busca,
                                            @RequestParam(defaultValue = "0") int pagina,
                                            @RequestParam(defaultValue = "20") int tamanho) {
        List<AreaComum> areas = areaComumService.listarParaGestao(usuarioLogado(), condominioId);
        // Como na tela: busca no nome e na descrição
        if (StringUtils.hasText(busca)) {
            String termo = busca.trim().toLowerCase();
            areas = areas.stream()
                    .filter(a -> a.getNome().toLowerCase().contains(termo)
                            || (a.getDescricao() != null && a.getDescricao().toLowerCase().contains(termo)))
                    .toList();
        }
        // Cada condomínio tem poucas áreas: a página é montada em memória, sobre a lista já filtrada
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100));
        int inicio = (int) Math.min(pageable.getOffset(), areas.size());
        int fim = Math.min(inicio + pageable.getPageSize(), areas.size());
        return Pagina.de(new PageImpl<>(areas.subList(inicio, fim), pageable, areas.size()), AreaComumResposta::de);
    }

    @GetMapping("/totais")
    @Operation(summary = "Quantidade de áreas comuns, ativas e inativas (sem o filtro de busca, como na tela)")
    public Map<String, Long> totais(@RequestParam(required = false) Integer condominioId) {
        List<AreaComum> areas = areaComumService.listarParaGestao(usuarioLogado(), condominioId);
        long ativas = areas.stream().filter(a -> Boolean.TRUE.equals(a.getAtiva())).count();
        return Map.of("TOTAL", (long) areas.size(), "ATIVAS", ativas, "INATIVAS", areas.size() - ativas);
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Condomínios em que quem está logado pode cadastrar áreas, e se pode gerenciar alguma")
    public OpcoesAreaComum opcoes() {
        Pessoa usuario = usuarioLogado();
        return new OpcoesAreaComum(
                condominioService.listarTodosCondominios(false).stream()
                        .filter(c -> areaComumService.podeGerenciar(usuario, c.getConCod()))
                        .map(CondominioOpcao::de)
                        .toList(),
                areaComumService.podeGerenciarAlgum(usuario));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Uma área comum, com todos os turnos (ativos e inativos)")
    public AreaComumResposta buscar(@PathVariable Integer id) {
        return AreaComumResposta.de(areaComumService.buscarParaGestao(id, usuarioLogado()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma área comum com os turnos")
    public AreaComumResposta cadastrar(@Valid @RequestBody AreaComumPedido pedido) {
        Pessoa usuario = usuarioLogado();
        AreaComum area = areaComumService.criar(pedido.paraServico(), usuario);
        return AreaComumResposta.de(areaComumService.buscarParaGestao(area.getAreCod(), usuario));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera a área comum e os turnos (turno com código é alterado, sem código é criado, "
            + "e o que não vier é removido). Para tirar a área de uso, envie ativa = false")
    public AreaComumResposta atualizar(@PathVariable Integer id, @Valid @RequestBody AreaComumPedido pedido) {
        Pessoa usuario = usuarioLogado();
        areaComumService.atualizar(id, pedido.paraServico(), usuario);
        return AreaComumResposta.de(areaComumService.buscarParaGestao(id, usuario));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui a área comum. Área que já foi reservada não pode ser excluída, só inativada")
    public void excluir(@PathVariable Integer id) {
        areaComumService.excluir(id, usuarioLogado());
    }

    private Pessoa usuarioLogado() {
        return pessoaService.getLoggedInUser();
    }

    public record OpcoesAreaComum(List<CondominioOpcao> condominios, boolean podeGerenciar) {
    }
}
