package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @GetMapping
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        model.addAttribute("nomeUsuarioLogado", userDetails.getPessoa().getPesNome());
        model.addAttribute("totalCondominios", 1);
        model.addAttribute("totalUnidades", 6);
        model.addAttribute("totalUsuarios", 10);
        model.addAttribute("currentPage", "dashboard");
        
        return "dashboard";
    }
}