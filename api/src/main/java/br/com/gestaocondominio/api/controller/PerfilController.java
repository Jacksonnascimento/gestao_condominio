package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/perfil")
@PreAuthorize("isAuthenticated()")
public class PerfilController {

    @Autowired
    private PessoaService pessoaService;

    @GetMapping("/modal")
    public String getModalPerfil(Model model) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        model.addAttribute("usuario", usuarioLogado);
        return "fragments/perfil-modal :: form-modal-content";
    }

    @PostMapping("/salvar-senha")
    @ResponseBody
    public ResponseEntity<?> salvarNovaSenha(@RequestParam String senhaAtual,
                                             @RequestParam String novaSenha,
                                             @RequestParam String confirmarNovaSenha) {
        try {
            Pessoa usuarioLogado = pessoaService.getLoggedInUser();

            if (!novaSenha.equals(confirmarNovaSenha)) {
                throw new IllegalArgumentException("A nova senha e a confirmação não conferem.");
            }
            
            pessoaService.atualizarSenha(usuarioLogado.getPesCod(), senhaAtual, novaSenha);

            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso!"));
        } catch (IllegalArgumentException | BadCredentialsException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro: " + e.getMessage()));
        }
    }
}