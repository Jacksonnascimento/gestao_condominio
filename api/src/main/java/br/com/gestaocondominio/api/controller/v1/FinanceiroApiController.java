package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.dto.BoletoDTO;
import br.com.gestaocondominio.api.controller.v1.dto.FinanceiroDTOs.BoletoAvulsoRequest;
import br.com.gestaocondominio.api.controller.v1.dto.FinanceiroDTOs.CondominioDisponivel;
import br.com.gestaocondominio.api.controller.v1.dto.FinanceiroDTOs.CondominioDoBoleto;
import br.com.gestaocondominio.api.controller.v1.dto.FinanceiroDTOs.OpcoesFinanceiro;
import br.com.gestaocondominio.api.controller.v1.dto.FinanceiroDTOs.PainelFinanceiro;
import br.com.gestaocondominio.api.controller.v1.dto.FinanceiroDTOs.UnidadePagadora;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.service.FinanceiroFakeService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Financeiro do condomínio. <strong>Ainda é demonstração</strong> ({@link FinanceiroFakeService}): os boletos são
 * gerados na hora, com valores e códigos fictícios, e os avulsos ficam só na memória da API.
 *
 * <p>Quem vê o quê: administrador geral, síndico e administração veem as cobranças de todas as unidades ativas do
 * condomínio e geram boleto avulso; os demais vinculados ao condomínio veem só as unidades que ocupam.</p>
 */
@RestController
@RequestMapping("/api/v1/financeiro")
@Tag(name = "Financeiro", description = "Demonstração: boletos fictícios, nada é cobrado de verdade")
public class FinanceiroApiController {

    private final FinanceiroFakeService financeiroService;
    private final PessoaService pessoaService;

    public FinanceiroApiController(FinanceiroFakeService financeiroService, PessoaService pessoaService) {
        this.financeiroService = financeiroService;
        this.pessoaService = pessoaService;
    }

    @GetMapping
    @Operation(summary = "Boletos em aberto, vencidos e histórico do condomínio (DADOS DE DEMONSTRAÇÃO)",
            description = "Os boletos são fictícios e gerados a cada consulta, com códigos diferentes a cada vez. Sem "
                    + "condominioId, vale o primeiro condomínio, em ordem de nome, de quem está logado.")
    public PainelFinanceiro painel(@RequestParam(required = false) Integer condominioId) {
        Pessoa usuario = usuarioLogado();
        Condominio condominio = financeiroService.condominioDoPainel(usuario, condominioId);
        List<Unidade> unidades = financeiroService.unidadesVisiveis(usuario, condominio.getConCod());
        return new PainelFinanceiro(true, CondominioDoBoleto.de(condominio),
                financeiroService.podeGerarBoleto(usuario, condominio.getConCod()),
                financeiroService.gerarBoletosAbertos(unidades),
                financeiroService.gerarBoletosVencidos(unidades),
                financeiroService.gerarHistorico(unidades));
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Condomínios e unidades pagadoras para a tela, e se quem está logado gera boleto avulso")
    public OpcoesFinanceiro opcoes(@RequestParam(required = false) Integer condominioId) {
        Pessoa usuario = usuarioLogado();
        List<Condominio> condominios = financeiroService.condominiosDisponiveis(usuario);
        if (condominios.isEmpty()) {
            return new OpcoesFinanceiro(List.of(), null, List.of(), false);
        }
        Condominio condominio = financeiroService.condominioDoPainel(usuario, condominioId);
        boolean podeGerarBoleto = financeiroService.podeGerarBoleto(usuario, condominio.getConCod());
        return new OpcoesFinanceiro(
                condominios.stream().map(CondominioDisponivel::de).toList(),
                condominio.getConCod(),
                podeGerarBoleto
                        ? financeiroService.unidadesVisiveis(usuario, condominio.getConCod()).stream()
                                .map(UnidadePagadora::de).toList()
                        : List.of(),
                podeGerarBoleto);
    }

    @PostMapping("/boletos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gera um boleto avulso para uma unidade (DEMONSTRAÇÃO: nada é cobrado de verdade)",
            description = "O boleto fica só na memória da API, até ela reiniciar, e aparece em \"Em Aberto\" para "
                    + "quem vê a unidade.")
    public BoletoDTO gerarBoleto(@Valid @RequestBody BoletoAvulsoRequest pedido) {
        return financeiroService.gerarBoletoAvulso(usuarioLogado(), pedido.unidadeId(), pedido.nomeTaxa().trim(),
                pedido.valor(), pedido.dataVencimento());
    }

    private Pessoa usuarioLogado() {
        return pessoaService.getLoggedInUser();
    }
}
