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
                model.addAttribute("pessoaIdLogado", usuarioLogado.getPesCod()); // <-- ADICIONADO

                boolean usuarioPodeGerenciar = false;
                boolean isUsuarioAdmin = false; // <-- ADICIONADO

                if (usuarioLogado.getPesIsGlobalAdmin()) {
                    usuarioPodeGerenciar = true;
                    isUsuarioAdmin = true; // <-- ADICIONADO
                } else {
                    Set<UserRole> roles = usuarioCondominioService.findByPessoa(usuarioLogado)
                            .stream()
                            .map(UsuarioCondominio::getUscPapel)
                            .collect(Collectors.toSet());
                    
                    if (!Collections.disjoint(roles, Set.of(UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM))) {
                        usuarioPodeGerenciar = true;
                    }
                    if (!Collections.disjoint(roles, Set.of(UserRole.SINDICO, UserRole.ADMIN))) { // <-- ADICIONADO (Sem FUNCIONARIO_ADM)
                        isUsuarioAdmin = true;
                    }
                }
                model.addAttribute("usuarioPodeGerenciar", usuarioPodeGerenciar);
                model.addAttribute("isUsuarioAdmin", isUsuarioAdmin); // <-- ADICIONADO

            } catch (Exception e) {
                model.addAttribute("nomeUsuarioLogado", "Usuário");
                model.addAttribute("usuarioPodeGerenciar", false);
                model.addAttribute("isUsuarioAdmin", false); // <-- ADICIONADO
                model.addAttribute("pessoaIdLogado", null); // <-- ADICIONADO
            }
        }
    }
}