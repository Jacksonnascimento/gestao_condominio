package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.v1.dto.OcupanteDTOs.AtualizarOcupanteRequest;
import br.com.gestaocondominio.api.controller.v1.dto.OcupanteDTOs.CadastrarOcupanteRequest;
import br.com.gestaocondominio.api.controller.v1.dto.OcupanteDTOs.OcupanteResposta;
import br.com.gestaocondominio.api.controller.v1.dto.OcupanteDTOs.OpcoesOcupante;
import br.com.gestaocondominio.api.controller.v1.dto.Opcao;
import br.com.gestaocondominio.api.controller.v1.dto.Pagina;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.TipoPeriodoOcupante;
import br.com.gestaocondominio.api.domain.service.OcupanteService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
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

import java.util.Map;

/**
 * Pessoas vinculadas às unidades (proprietários, locatários, dependentes...). Quem vê e quem altera é decidido pelo
 * {@link OcupanteService}: síndico, administradora e funcionário administrativo gerenciam os ocupantes do seu
 * condomínio (o administrador geral, de todos); o morador só consulta os ocupantes das unidades que ocupa.
 */
@RestController
@RequestMapping("/api/v1/ocupantes")
@Tag(name = "Ocupantes")
public class OcupanteApiController {

    private final OcupanteService ocupanteService;
    private final PessoaService pessoaService;

    public OcupanteApiController(OcupanteService ocupanteService, PessoaService pessoaService) {
        this.ocupanteService = ocupanteService;
        this.pessoaService = pessoaService;
    }

    @GetMapping
    @Operation(summary = "Lista os ocupantes visíveis para quem está logado, em ordem alfabética")
    public Pagina<OcupanteResposta> listar(@RequestParam(required = false) Integer condominioId,
                                           @RequestParam(required = false) String busca,
                                           @RequestParam(required = false) OcupanteVinculo vinculo,
                                           @RequestParam(required = false) Integer unidadeId,
                                           @RequestParam(defaultValue = "0") int pagina,
                                           @RequestParam(defaultValue = "20") int tamanho) {
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100));
        return Pagina.de(ocupanteService.consultarOcupantes(pessoaService.getLoggedInUser(), condominioId, busca,
                vinculo, unidadeId, pageable), ocupante -> OcupanteResposta.de(ocupante, false));
    }

    @GetMapping("/totais")
    @Operation(summary = "Quantidade de ocupantes por vínculo, com os mesmos filtros da listagem")
    public Map<String, Long> totais(@RequestParam(required = false) Integer condominioId,
                                    @RequestParam(required = false) String busca,
                                    @RequestParam(required = false) OcupanteVinculo vinculo,
                                    @RequestParam(required = false) Integer unidadeId) {
        return ocupanteService.contarOcupantesPorVinculo(pessoaService.getLoggedInUser(), condominioId, busca,
                vinculo, unidadeId);
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Vínculos e tipos de período para o formulário, e se quem está logado pode cadastrar ocupantes")
    public OpcoesOcupante opcoes() {
        return new OpcoesOcupante(
                Opcao.de(OcupanteVinculo.class, OcupanteVinculo::getDescricao),
                Opcao.de(TipoPeriodoOcupante.class, TipoPeriodoOcupante::getDescricao),
                ocupanteService.podeGerenciarOcupantes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Um ocupante; o CPF/CNPJ só vem para quem gerencia o condomínio")
    public OcupanteResposta buscar(@PathVariable Integer id) {
        return resposta(ocupanteService.buscarOcupanteVisivel(id, pessoaService.getLoggedInUser()));
    }

    /** Para saber se o CPF/CNPJ já tem cadastro antes de preencher o resto, use {@code /api/v1/pessoas}. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vincula uma pessoa a uma unidade, cadastrando a pessoa se o CPF/CNPJ for novo")
    public OcupanteResposta cadastrar(@Valid @RequestBody CadastrarOcupanteRequest pedido) {
        return resposta(ocupanteService.cadastrarOcupanteComoGestor(pedido.paraDto()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera o vínculo e os dados de contato do ocupante")
    public OcupanteResposta atualizar(@PathVariable Integer id, @Valid @RequestBody AtualizarOcupanteRequest pedido) {
        return resposta(ocupanteService.editarOcupanteComoGestor(id, pedido.paraDto()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desfaz o vínculo da pessoa com a unidade (a pessoa continua cadastrada)")
    public void excluir(@PathVariable Integer id) {
        ocupanteService.excluirOcupanteComoGestor(id);
    }

    private OcupanteResposta resposta(Ocupante ocupante) {
        return OcupanteResposta.de(ocupante,
                ocupanteService.podeGerenciarOcupantesDoCondominio(ocupante.getUnidade().getCondominio().getConCod()));
    }
}
