package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.OcupanteResponseDTO;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.StatusContrato;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired private PessoaService pessoaService;
    @Autowired private UnidadeService unidadeService;
    @Autowired private OcupanteService ocupanteService;
    @Autowired private ContratoService contratoService;
    @Autowired private OcorrenciaService ocorrenciaService;
    @Autowired private UsuarioCondominioService usuarioCondominioService;

    @GetMapping
    public String dashboard(Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        
        // Verificação de segurança: Morador não acessa dashboard
        boolean isGerencial = usuarioLogado.getPesIsGlobalAdmin() || 
                              usuarioCondominioService.possuiRole(usuarioLogado, UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO);
        
        if (!isGerencial) {
            return "redirect:/unidades";
        }
        
        // Determina o contexto do condomínio para filtros (se não for Global Admin)
        Integer condominioId = null;
        if (!usuarioLogado.getPesIsGlobalAdmin()) {
            condominioId = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
        }

        // 1. Total de Unidades
        // Reutiliza a lógica de listagem que já aplica as regras de permissão
        List<Unidade> unidades = unidadeService.listarTodasUnidades(false, null, null);
        int totalUnidades = unidades.size();

        // 2. Ocupantes Ativos
        List<OcupanteResponseDTO> ocupantes = ocupanteService.consultarOcupantesPorUsuario(usuarioLogado, condominioId, null, null);
        int totalOcupantes = ocupantes.size();

        // 3. Funcionários (Estático por enquanto)
        int totalFuncionarios = 0;

        // 4. Contratos Ativos
        Map<StatusContrato, Long> contratosMap = contratoService.contarContratosPorStatus(condominioId);
        long totalContratosAtivos = contratosMap.getOrDefault(StatusContrato.ATIVO, 0L);

        // 5. Ocorrências Pendentes (Abertas + Em Análise)
        Map<String, Long> ocorrenciasMap = ocorrenciaService.contarOcorrenciasPorStatusEPeriodo(
                usuarioLogado, condominioId, null, null, null, null, null, null
        );
        long totalOcorrenciasPendentes = ocorrenciasMap.getOrDefault("ABERTA", 0L) + 
                                         ocorrenciasMap.getOrDefault("EM_ANALISE", 0L);

        model.addAttribute("nomeUsuarioLogado", usuarioLogado.getPesNome());
        model.addAttribute("totalUnidades", totalUnidades);
        model.addAttribute("totalOcupantes", totalOcupantes);
        model.addAttribute("totalUsuarios", totalFuncionarios); // Mantém o nome da variável usado no template para funcionários
        model.addAttribute("totalContratosAtivos", totalContratosAtivos);
        model.addAttribute("totalOcorrenciasPendentes", totalOcorrenciasPendentes);
        model.addAttribute("currentPage", "dashboard");
        
        return "dashboard";
    }
}