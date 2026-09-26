package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.v1.dto.DashboardDTOs.Painel;
import br.com.gestaocondominio.api.domain.service.DashboardService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Painel inicial da administração e da portaria. Quem não é de nenhuma das duas (o morador, que a tela antiga manda
 * para as unidades) recebe 403; o sistema web decide para onde levar cada pessoa pelos vínculos devolvidos no login.
 * As regras de cada indicador estão no {@link DashboardService}.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;
    private final PessoaService pessoaService;

    public DashboardApiController(DashboardService dashboardService, PessoaService pessoaService) {
        this.dashboardService = dashboardService;
        this.pessoaService = pessoaService;
    }

    @GetMapping
    @Operation(summary = "Indicadores do painel: unidades, ocupantes, contratos ativos e ocorrências pendentes")
    public Painel painel(@RequestParam(required = false) Integer condominioId) {
        return dashboardService.montarPainel(pessoaService.getLoggedInUser(), condominioId);
    }
}
