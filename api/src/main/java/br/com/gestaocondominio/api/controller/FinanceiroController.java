package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.BoletoGeracaoRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import br.com.gestaocondominio.api.domain.service.FinanceiroFakeService;
import br.com.gestaocondominio.api.domain.service.OcupanteService;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/financeiro")
public class FinanceiroController {

    private final FinanceiroFakeService financeiroService;
    private final UnidadeService unidadeService;
    private final CondominioService condominioService;
    private final UsuarioCondominioService usuarioCondominioService;
    private final OcupanteService ocupanteService;

    public FinanceiroController(FinanceiroFakeService financeiroService,
                                UnidadeService unidadeService,
                                CondominioService condominioService,
                                UsuarioCondominioService usuarioCondominioService,
                                OcupanteService ocupanteService) {
        this.financeiroService = financeiroService;
        this.unidadeService = unidadeService;
        this.condominioService = condominioService;
        this.usuarioCondominioService = usuarioCondominioService;
        this.ocupanteService = ocupanteService;
    }

    @GetMapping
    public String index(Model model,
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @RequestParam(name = "condominioId", required = false) Integer condominioId,
                        HttpSession session) {

        // 1. Tenta usar o ID passado na URL
        Integer idCondominioFinal = condominioId;

        // 2. Se não veio na URL, tenta extrair das permissões do usuário
        if (idCondominioFinal == null) {
            idCondominioFinal = extrairIdCondominio(userDetails);
        }

        // 3. Se ainda for null (ex: Global Admin), pega o primeiro do banco
        if (idCondominioFinal == null && Boolean.TRUE.equals(userDetails.getPessoa().getPesIsGlobalAdmin())) {
            List<Condominio> todos = condominioService.findAll();
            if (!todos.isEmpty()) {
                idCondominioFinal = todos.get(0).getConCod();
            }
        }

        if (idCondominioFinal == null) {
            return "redirect:/dashboard";
        }
        
        Optional<Condominio> condominioOpt = condominioService.buscarCondominioPorId(idCondominioFinal);
        if (condominioOpt.isEmpty()) {
             return "redirect:/dashboard";
        }
        Condominio condominio = condominioOpt.get();

        boolean isMorador = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_MORADOR"));

        List<Unidade> unidadesAlvo;

        if (isMorador) {
            // CORREÇÃO: Busca as unidades vinculadas ao morador via OcupanteService
            List<Unidade> todasUnidadesMorador = ocupanteService.findUnidadesByMorador(userDetails.getPessoa());
            
            // Filtra apenas as unidades deste condomínio específico (caso o morador tenha imóveis em outros locais)
            unidadesAlvo = todasUnidadesMorador.stream()
                    .filter(u -> u.getCondominio().getConCod().equals(condominio.getConCod()))
                    .collect(Collectors.toList());
        } else {
            // Se for gestor, busca todas do condomínio
            unidadesAlvo = unidadeService.findByCondominioId(idCondominioFinal);
        }

        model.addAttribute("boletosAbertos", financeiroService.gerarBoletosAbertos(unidadesAlvo, session));
        model.addAttribute("boletosVencidos", financeiroService.gerarBoletosVencidos(unidadesAlvo));
        model.addAttribute("boletosHistorico", financeiroService.gerarHistorico(unidadesAlvo));
        
        model.addAttribute("condominio", condominio);
        model.addAttribute("isMorador", isMorador);
        
        if (!isMorador) {
             model.addAttribute("todasUnidades", unidadeService.findByCondominioId(idCondominioFinal));
        }

        return "financeiro";
    }

    @PostMapping("/gerar")
    public Object gerarBoletoManual(Model model,
                                    BoletoGeracaoRequestDTO dto,
                                    HttpSession session) {
        try {
            Unidade unidade = unidadeService.buscarUnidadePorId(dto.getUnidadeId())
                    .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada"));
            
            if (dto.getDataVencimento() == null) {
                dto.setDataVencimento(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()));
            }

            financeiroService.salvarBoletoManual(session, unidade, dto.getNomeTaxa(), dto.getValor(), dto.getDataVencimento());

            return ResponseEntity.ok().body(Map.of("message", "Boleto gerado com sucesso (Modo Demonstração)"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Erro ao gerar boleto: " + e.getMessage()));
        }
    }

    private Integer extrairIdCondominio(UserDetailsImpl userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.contains("_") && a.matches(".*_\\d+$"))
                .map(a -> a.substring(a.lastIndexOf('_') + 1))
                .map(Integer::parseInt)
                .findFirst()
                .orElse(null);
    }
}