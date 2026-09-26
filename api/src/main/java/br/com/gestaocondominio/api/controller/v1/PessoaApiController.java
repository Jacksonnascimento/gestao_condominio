package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.v1.dto.PessoaDTOs.ConsultaPorDocumentoRequest;
import br.com.gestaocondominio.api.controller.v1.dto.PessoaDTOs.PessoaResumo;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de pessoas para os formulários de cadastro (ex.: vincular um ocupante). Só para quem gerencia ocupantes em
 * algum condomínio; não há listagem de pessoas, e cada consulta exige o CPF/CNPJ completo.
 */
@RestController
@RequestMapping("/api/v1/pessoas")
@Tag(name = "Pessoas")
public class PessoaApiController {

    private final PessoaService pessoaService;

    public PessoaApiController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    /**
     * O CPF/CNPJ vai no corpo, e não no endereço, para não ficar gravado em logs de acesso e de proxies. Responde 404
     * quando não há cadastro, e o formulário segue pedindo os dados da pessoa nova.
     */
    @PostMapping("/consulta-por-documento")
    @PreAuthorize("@ocupanteService.podeGerenciarOcupantes()")
    @Operation(summary = "Busca a pessoa pelo CPF/CNPJ (quem gerencia ocupantes)")
    public PessoaResumo consultarPorDocumento(@Valid @RequestBody ConsultaPorDocumentoRequest pedido) {
        return pessoaService.buscarPorCpfCnpj(pedido.cpfCnpj().replaceAll("[^0-9]", ""))
                .map(PessoaResumo::de)
                .orElseThrow(() -> new EntityNotFoundException("Nenhuma pessoa cadastrada com este CPF/CNPJ."));
    }
}
