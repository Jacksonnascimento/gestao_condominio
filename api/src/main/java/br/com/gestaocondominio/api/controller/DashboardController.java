package br.com.gestaocondominio.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalCondominios", 1);
        model.addAttribute("totalUnidades", 6);
        model.addAttribute("totalUsuarios", 10);
        
        // Linha adicionada para informar a página atual
        model.addAttribute("currentPage", "dashboard");
        
        return "dashboard";
    }
}