package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @GetMapping("/esqueci-senha")
    public String showForgotPasswordPage() {
        return "esqueci-senha";
    }

    @PostMapping("/esqueci-senha")
    public String processForgotPassword(@RequestParam("email") String userEmail, RedirectAttributes attributes) {
        try {
            passwordResetService.createPasswordResetToken(userEmail, 1); // 1 Hora de expiração
            attributes.addFlashAttribute("successMessage", "Um link para redefinição de senha foi enviado para o seu e-mail.");
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("errorMessage", "Erro ao processar a solicitação. Tente novamente.");
        }
        return "redirect:/esqueci-senha";
    }

    @GetMapping("/definir-senha")
    public String showChangePasswordPage(@RequestParam("token") String token, Model model, RedirectAttributes attributes) {
        try {
            passwordResetService.validatePasswordResetToken(token);
            model.addAttribute("token", token);
            return "definir-senha";
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login?tokenError=true";
        }
    }

    @PostMapping("/definir-senha")
    public String processChangePassword(@RequestParam("token") String token,
                                        @RequestParam("senha") String newPassword,
                                        @RequestParam("confirmarSenha") String confirmPassword,
                                        RedirectAttributes attributes) {

        if (!newPassword.equals(confirmPassword)) {
            attributes.addFlashAttribute("errorMessage", "As senhas não conferem.");
            return "redirect:/definir-senha?token=" + token;
        }

        try {
            passwordResetService.resetPassword(token, newPassword);
            attributes.addFlashAttribute("successMessage", "Senha alterada com sucesso! Você já pode fazer o login.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login?tokenError=true";
        }
    }
}