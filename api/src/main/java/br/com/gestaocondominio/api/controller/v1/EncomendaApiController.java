package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.dto.EncomendaDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaRequestDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaRetiradaRequestDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaStatusRequestDTO;
import br.com.gestaocondominio.api.controller.v1.dto.Opcao;
import br.com.gestaocondominio.api.controller.v1.dto.Pagina;
import br.com.gestaocondominio.api.domain.entity.Encomenda;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.EncomendaStatus;
import br.com.gestaocondominio.api.domain.enums.EncomendaTipo;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.EncomendaService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
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

import java.util.List;
import java.util.Map;

/**
 * Encomendas recebidas na portaria. Quem vê e quem altera cada encomenda é decidido pelo {@link EncomendaService}, a
 * partir de quem está logado: o morador vê as da sua unidade; síndico, administração e portaria gerenciam as do
 * condomínio.
 */
@RestController
@RequestMapping("/api/v1/encomendas")
@Tag(name = "Encomendas")
public class EncomendaApiController {

    private static final List<EncomendaStatus> STATUS_DE_ATUALIZACAO =
            List.of(EncomendaStatus.PENDENTE, EncomendaStatus.DEVOLVIDA, EncomendaStatus.EXTRAVIADA);

    private final EncomendaService encomendaService;
    private final PessoaService pessoaService;
    private final UsuarioCondominioService usuarioCondominioService;

    public EncomendaApiController(EncomendaService encomendaService, PessoaService pessoaService,
                                  UsuarioCondominioService usuarioCondominioService) {
        this.encomendaService = encomendaService;
        this.pessoaService = pessoaService;
        this.usuarioCondominioService = usuarioCondominioService;
    }

    @GetMapping
    @Operation(summary = "Lista as encomendas visíveis para quem está logado, das mais recentes para as mais antigas")
    public Pagina<EncomendaDTO> listar(@RequestParam(required = false) Integer condominioId,
                                       @RequestParam(required = false) String busca,
                                       @RequestParam(required = false) Integer unidadeId,
                                       @RequestParam(required = false) EncomendaStatus status,
                                       @RequestParam(defaultValue = "0") int pagina,
                                       @RequestParam(defaultValue = "20") int tamanho) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100),
                Sort.by(Sort.Direction.DESC, "dataRecebimento"));
        return Pagina.de(encomendaService.consultarEncomendas(usuarioLogado(), condominioId, busca, unidadeId,
                status, pageable));
    }

    @GetMapping("/totais")
    @Operation(summary = "Quantidade de encomendas por situação, com os mesmos filtros da listagem")
    public Map<String, Long> totais(@RequestParam(required = false) Integer condominioId,
                                    @RequestParam(required = false) String busca,
                                    @RequestParam(required = false) Integer unidadeId,
                                    @RequestParam(required = false) EncomendaStatus status) {
        return encomendaService.contarStatusEncomendas(usuarioLogado(), condominioId, busca, unidadeId, status);
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Tipos e situações para os formulários, e se quem está logado pode registrar encomendas")
    public OpcoesEncomenda opcoes() {
        Pessoa usuario = usuarioLogado();
        return new OpcoesEncomenda(
                Opcao.de(EncomendaTipo.class, EncomendaTipo::getDescricao),
                Opcao.de(EncomendaStatus.class, EncomendaStatus::getDescricao),
                STATUS_DE_ATUALIZACAO.stream().map(s -> new Opcao(s.name(), s.getDescricao())).toList(),
                podeGerenciar(usuario));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Uma encomenda")
    public EncomendaDTO buscar(@PathVariable Long id) {
        return encomendaService.buscarPorIdDTO(id, usuarioLogado());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma encomenda recebida")
    public EncomendaDTO registrar(@Valid @RequestBody EncomendaRequestDTO pedido) {
        Pessoa usuario = usuarioLogado();
        Encomenda encomenda = encomendaService.criarEncomenda(pedido, usuario);
        return encomendaService.buscarPorIdDTO(encomenda.getEncCod(), usuario);
    }

    @PostMapping("/{id}/retirada")
    @Operation(summary = "Registra a retirada da encomenda")
    public EncomendaDTO registrarRetirada(@PathVariable Long id, @Valid @RequestBody EncomendaRetiradaRequestDTO pedido) {
        Pessoa usuario = usuarioLogado();
        encomendaService.registrarRetirada(id, pedido, usuario);
        return encomendaService.buscarPorIdDTO(id, usuario);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Muda a situação da encomenda (pendente, devolvida ou extraviada)")
    public EncomendaDTO atualizarStatus(@PathVariable Long id, @Valid @RequestBody EncomendaStatusRequestDTO pedido) {
        Pessoa usuario = usuarioLogado();
        encomendaService.atualizarStatus(id, pedido, usuario);
        return encomendaService.buscarPorIdDTO(id, usuario);
    }

    private boolean podeGerenciar(Pessoa usuario) {
        return Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin()) || usuarioCondominioService.possuiRole(usuario,
                UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO);
    }

    private Pessoa usuarioLogado() {
        return pessoaService.getLoggedInUser();
    }

    public record OpcoesEncomenda(List<Opcao> tipos, List<Opcao> status, List<Opcao> statusDeAtualizacao,
                                  boolean podeGerenciar) {
    }
}
