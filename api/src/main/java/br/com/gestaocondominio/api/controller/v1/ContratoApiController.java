package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.v1.dto.ContratoDTOs.AbaContrato;
import br.com.gestaocondominio.api.controller.v1.dto.ContratoDTOs.CondominioDisponivel;
import br.com.gestaocondominio.api.controller.v1.dto.ContratoDTOs.ContratoRequest;
import br.com.gestaocondominio.api.controller.v1.dto.ContratoDTOs.ContratoResponse;
import br.com.gestaocondominio.api.controller.v1.dto.ContratoDTOs.OpcoesContrato;
import br.com.gestaocondominio.api.controller.v1.dto.ContratoDTOs.TotaisContrato;
import br.com.gestaocondominio.api.controller.v1.dto.Opcao;
import br.com.gestaocondominio.api.controller.v1.dto.Pagina;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.StatusContrato;
import br.com.gestaocondominio.api.domain.service.ContratoService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Contratos do condomínio com prestadores de serviço. Quem vê e quem altera é decidido pelo {@link ContratoService}:
 * o administrador geral, em todos os condomínios; síndico, administração e funcionário administrativo, só nos
 * condomínios em que têm esse papel. Os demais não acessam contratos.
 */
@RestController
@RequestMapping("/api/v1/contratos")
@Tag(name = "Contratos")
public class ContratoApiController {

    private static final List<StatusContrato> STATUS_DO_HISTORICO =
            List.of(StatusContrato.FINALIZADO, StatusContrato.RESCINDIDO);
    private static final List<StatusContrato> STATUS_DE_CADASTRO =
            List.of(StatusContrato.ATIVO, StatusContrato.FINALIZADO, StatusContrato.RESCINDIDO);

    private final ContratoService contratoService;
    private final PessoaService pessoaService;

    public ContratoApiController(ContratoService contratoService, PessoaService pessoaService) {
        this.contratoService = contratoService;
        this.pessoaService = pessoaService;
    }

    @GetMapping
    @Operation(summary = "Lista os contratos de uma aba (ativos, próximos a vencer ou histórico), por início e fim")
    public Pagina<ContratoResponse> listar(@RequestParam(defaultValue = "ATIVOS") AbaContrato aba,
                                           @RequestParam(required = false) Integer condominioId,
                                           @RequestParam(required = false) String busca,
                                           @RequestParam(required = false) StatusContrato status,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicioApos,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fimAntes,
                                           @RequestParam(defaultValue = "0") int pagina,
                                           @RequestParam(defaultValue = "20") int tamanho) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100));
        return Pagina.de(contratoService.consultarContratos(usuarioLogado(), condominioId, busca, status,
                aba == AbaContrato.A_VENCER, aba == AbaContrato.HISTORICO, inicioApos, fimAntes, pageable),
                ContratoResponse::de);
    }

    @GetMapping("/totais")
    @Operation(summary = "Quantidade de contratos por situação, para os cartões do topo da tela")
    public TotaisContrato totais(@RequestParam(required = false) Integer condominioId) {
        return TotaisContrato.de(contratoService.contarContratosPorStatus(usuarioLogado(), condominioId));
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Condomínios, abas e situações para a tela, e se quem está logado gerencia contratos")
    public OpcoesContrato opcoes() {
        Pessoa usuario = usuarioLogado();
        boolean podeGerenciar = contratoService.podeGerenciar(usuario);
        return new OpcoesContrato(
                podeGerenciar
                        ? contratoService.condominiosDisponiveis(usuario).stream().map(CondominioDisponivel::de).toList()
                        : List.of(),
                Opcao.de(AbaContrato.class, AbaContrato::getDescricao),
                Opcao.de(StatusContrato.class, StatusContrato::getDescricao),
                STATUS_DO_HISTORICO.stream().map(s -> new Opcao(s.name(), s.getDescricao())).toList(),
                STATUS_DE_CADASTRO.stream().map(s -> new Opcao(s.name(), s.getDescricao())).toList(),
                podeGerenciar);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Um contrato")
    public ContratoResponse buscar(@PathVariable Long id) {
        return ContratoResponse.de(contratoService.buscarContrato(id, usuarioLogado()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um contrato")
    public ContratoResponse criar(@Valid @RequestBody ContratoRequest pedido) {
        return ContratoResponse.de(contratoService.criarContrato(pedido.paraDTO(), usuarioLogado()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera um contrato (o condomínio dele não muda)")
    public ContratoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ContratoRequest pedido) {
        return ContratoResponse.de(contratoService.atualizarContrato(id, pedido.paraDTO(), usuarioLogado()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um contrato")
    public void excluir(@PathVariable Long id) {
        contratoService.excluirContrato(id, usuarioLogado());
    }

    private Pessoa usuarioLogado() {
        return pessoaService.getLoggedInUser();
    }
}
