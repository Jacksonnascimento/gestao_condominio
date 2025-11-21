package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private PessoaService pessoaService;

    @Autowired
    private UsuarioCondominioService usuarioCondominioService;

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            try {
                Pessoa usuarioLogado = pessoaService.getLoggedInUser();
                model.addAttribute("nomeUsuarioLogado", usuarioLogado.getPesNome());
                model.addAttribute("pessoaIdLogado", usuarioLogado.getPesCod()); 

                boolean usuarioPodeGerenciar = false;
                boolean isUsuarioAdmin = false; 
                boolean usuarioPodeGerenciarEncomendas = false;
                boolean usuarioPodeGerenciarVisitantes = false; // Apenas Gestores e Porteiros
                boolean usuarioPodeVisualizarVisitantes = false; // Gestores, Porteiros e Moradores

                if (Boolean.TRUE.equals(usuarioLogado.getPesIsGlobalAdmin())) {
                    usuarioPodeGerenciar = true;
                    isUsuarioAdmin = true; 
                    usuarioPodeGerenciarEncomendas = true;
                    usuarioPodeGerenciarVisitantes = true;
                    usuarioPodeVisualizarVisitantes = true;
                } else {
                    Set<UserRole> roles = usuarioCondominioService.findByPessoa(usuarioLogado)
                            .stream()
                            .map(UsuarioCondominio::getUscPapel)
                            .collect(Collectors.toSet());
                    
                    // Permissão Geral (Contratos, etc)
                    if (!Collections.disjoint(roles, Set.of(UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM))) {
                        usuarioPodeGerenciar = true;
                    }
                    // Permissão Admin (Usuários)
                    if (!Collections.disjoint(roles, Set.of(UserRole.SINDICO, UserRole.ADMIN))) { 
                        isUsuarioAdmin = true;
                    }
                    // Permissão Operacional (Encomendas e Visitantes - Escrita)
                    if (!Collections.disjoint(roles, Set.of(UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO))) {
                        usuarioPodeGerenciarEncomendas = true;
                        usuarioPodeGerenciarVisitantes = true;
                    }
                    // Permissão de Visualização de Visitantes (Inclui Morador)
                    if (!Collections.disjoint(roles, Set.of(UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO, UserRole.MORADOR))) {
                        usuarioPodeVisualizarVisitantes = true;
                    }
                }
                model.addAttribute("usuarioPodeGerenciar", usuarioPodeGerenciar);
                model.addAttribute("isUsuarioAdmin", isUsuarioAdmin); 
                model.addAttribute("usuarioPodeGerenciarEncomendas", usuarioPodeGerenciarEncomendas);
                
                // Flags específicas para Visitantes
                model.addAttribute("usuarioPodeGerenciarVisitantes", usuarioPodeGerenciarVisitantes); // Controla botões (Novo, Editar, Saída)
                model.addAttribute("usuarioPodeVisualizarVisitantes", usuarioPodeVisualizarVisitantes); // Controla acesso ao Menu

            } catch (Exception e) {
                model.addAttribute("nomeUsuarioLogado", "Usuário");
                model.addAttribute("usuarioPodeGerenciar", false);
                model.addAttribute("isUsuarioAdmin", false); 
                model.addAttribute("pessoaIdLogado", null); 
                model.addAttribute("usuarioPodeGerenciarEncomendas", false);
                model.addAttribute("usuarioPodeGerenciarVisitantes", false);
                model.addAttribute("usuarioPodeVisualizarVisitantes", false);
            }
        }
    }
}