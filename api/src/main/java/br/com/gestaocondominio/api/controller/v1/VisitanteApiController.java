package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.v1.dto.Opcao;
import br.com.gestaocondominio.api.controller.v1.dto.Pagina;
import br.com.gestaocondominio.api.controller.v1.dto.VisitanteDTOs.CondominioOpcao;
import br.com.gestaocondominio.api.controller.v1.dto.VisitanteDTOs.MoradorOpcao;
import br.com.gestaocondominio.api.controller.v1.dto.VisitanteDTOs.UnidadeOpcao;
import br.com.gestaocondominio.api.controller.v1.dto.VisitanteDTOs.VisitanteDetalhe;
import br.com.gestaocondominio.api.controller.v1.dto.VisitanteDTOs.VisitantePedido;
import br.com.gestaocondominio.api.controller.v1.dto.VisitanteDTOs.VisitanteResumo;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Visitante;
import br.com.gestaocondominio.api.domain.enums.VisitanteStatus;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.VisitanteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
import java.util.Map;

/**
 * Entrada e saída de visitantes na portaria. Quem vê e quem altera cada registro é decidido pelo
 * {@link VisitanteService}, a partir de quem está logado: administração e portaria registram e alteram os visitantes
 * do condomínio em que trabalham; o morador só consulta os das unidades que ocupa, sem CPF, RG nem observações.
 */
@RestController
@RequestMapping("/api/v1/visitantes")
@Tag(name = "Visitantes")
public class VisitanteApiController {

    private final VisitanteService visitanteService;
    private final PessoaService pessoaService;
    private final CondominioRepository condominioRepository;

    public VisitanteApiController(VisitanteService visitanteService, PessoaService pessoaService,
                                  CondominioRepository condominioRepository) {
        this.visitanteService = visitanteService;
        this.pessoaService = pessoaService;
        this.condominioRepository = condominioRepository;
    }

    @GetMapping
    @Operation(summary = "Lista os visitantes visíveis para quem está logado, das entradas mais recentes para as mais "
            + "antigas; com status, só os que estão no condomínio (NO_LOCAL) ou só os que saíram (SAIU)")
    public Pagina<VisitanteResumo> listar(@RequestParam(required = false) Integer condominioId,
                                          @RequestParam(required = false) String busca,
                                          @RequestParam(required = false) Integer unidadeId,
                                          @RequestParam(required = false) VisitanteStatus status,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "20") int tamanho) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100),
                Sort.by(Sort.Direction.DESC, "dataEntrada"));
        return Pagina.de(visitanteService.consultarResumos(usuarioLogado(), condominioId, busca, unidadeId, status,
                pageable));
    }

    /** Chaves: {@code TOTAL}, {@code NO_LOCAL}, {@code DO_DIA} (entradas hoje) e {@code SAIDAS_DIA} (saídas hoje). */
    @GetMapping("/totais")
    @Operation(summary = "Total de registros, visitantes no local e entradas e saídas de hoje, com os filtros da listagem")
    public Map<String, Long> totais(@RequestParam(required = false) Integer condominioId,
                                    @RequestParam(required = false) String busca,
                                    @RequestParam(required = false) Integer unidadeId) {
        return visitanteService.contarVisitantes(usuarioLogado(), condominioId, busca, unidadeId);
    }

    /**
     * Situações, condomínios e unidades para o filtro e o formulário, e se quem está logado pode registrar
     * visitantes. Os condomínios são todos os ativos para o administrador geral e, para os demais, aqueles em que a
     * pessoa trabalha na administração ou na portaria. Com {@code condominioId}, as unidades são só as dele.
     */
    @GetMapping("/opcoes")
    @Operation(summary = "Listas de escolha da tela de visitantes e se quem está logado pode registrá-los")
    public OpcoesVisitante opcoes(@RequestParam(required = false) Integer condominioId) {
        Pessoa usuario = usuarioLogado();
        return new OpcoesVisitante(
                Opcao.de(VisitanteStatus.class, VisitanteStatus::getDescricao),
                condominiosDisponiveis(usuario),
                visitanteService.unidadesDisponiveis(usuario, condominioId),
                visitanteService.podeGerenciarVisitantes(usuario));
    }

    @GetMapping("/opcoes/moradores")
    @Operation(summary = "Ocupantes da unidade, para indicar quem autorizou a entrada")
    public List<MoradorOpcao> moradores(@RequestParam Integer unidadeId) {
        return visitanteService.moradoresDaUnidade(unidadeId, usuarioLogado());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Um visitante")
    public VisitanteDetalhe buscar(@PathVariable Integer id) {
        return visitanteService.buscarDetalhe(id, usuarioLogado());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra a entrada de um visitante")
    public VisitanteDetalhe registrarEntrada(@Valid @RequestBody VisitantePedido pedido) {
        Pessoa usuario = usuarioLogado();
        Visitante visitante = visitanteService.cadastrarVisitante(pedido.paraRequisicao(), usuario);
        return visitanteService.buscarDetalhe(visitante.getVisCod(), usuario);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera os dados do visitante")
    public VisitanteDetalhe editar(@PathVariable Integer id, @Valid @RequestBody VisitantePedido pedido) {
        Pessoa usuario = usuarioLogado();
        visitanteService.atualizarVisitante(id, pedido.paraRequisicao(), usuario);
        return visitanteService.buscarDetalhe(id, usuario);
    }

    @PostMapping("/{id}/saida")
    @Operation(summary = "Registra a saída do visitante")
    public VisitanteDetalhe registrarSaida(@PathVariable Integer id) {
        Pessoa usuario = usuarioLogado();
        visitanteService.registrarSaida(id, usuario);
        return visitanteService.buscarDetalhe(id, usuario);
    }

    private List<CondominioOpcao> condominiosDisponiveis(Pessoa usuario) {
        List<Condominio> condominios = Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin())
                ? condominioRepository.findByConAtivo(true)
                : condominioRepository.findAllById(visitanteService.condominiosQueGerencia(usuario));
        return condominios.stream()
                .sorted(Comparator.comparing(Condominio::getConNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(CondominioOpcao::de)
                .toList();
    }

    private Pessoa usuarioLogado() {
        return pessoaService.getLoggedInUser();
    }

    public record OpcoesVisitante(List<Opcao> status, List<CondominioOpcao> condominios, List<UnidadeOpcao> unidades,
                                  boolean podeGerenciar) {
    }
}
