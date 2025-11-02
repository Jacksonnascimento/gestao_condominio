package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.PasswordResetToken;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.PasswordResetTokenRepository;
import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired private PessoaRepository pessoaRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioCondominioRepository usuarioCondominioRepository;

    @Transactional
    public void createPasswordResetToken(String email, int expiryHours) {
        Pessoa pessoa = pessoaRepository.findByPesEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Não foi encontrado um usuário com o e-mail: " + email));

        
        if (Boolean.FALSE.equals(pessoa.getPesIsGlobalAdmin())) {
            boolean hasActiveRole = usuarioCondominioRepository.findByPessoa(pessoa)
                                        .stream()
                                        .anyMatch(uc -> uc.getUscAtivoAssociacao());
            
            if (!hasActiveRole) {
                throw new IllegalArgumentException("Este e-mail pertence a um cadastro, mas não possui permissão de login no sistema.");
            }
        }
        

        tokenRepository.deleteByPessoaId(pessoa.getPesCod());

        String token = UUID.randomUUID().toString();
        PasswordResetToken myToken = new PasswordResetToken(token, pessoa, expiryHours);
        tokenRepository.save(myToken);

        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String resetUrl = baseUrl + "/definir-senha?token=" + token;

        emailService.sendPasswordResetEmail(pessoa.getPesEmail(), resetUrl, pessoa.getPesNome());
    }

    @Transactional(readOnly = true)
    public String validatePasswordResetToken(String token) {
        PasswordResetToken passToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou não encontrado."));

        if (passToken.isExpired()) {
            throw new IllegalArgumentException("Seu link de redefinição de senha expirou. Por favor, solicite um novo.");
        }

        return "Token válido.";
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken passToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou não encontrado."));

        if (passToken.isExpired()) {
            tokenRepository.delete(passToken);
            throw new IllegalArgumentException("Seu link de redefinição de senha expirou. Por favor, solicite um novo.");
        }

        Pessoa pessoa = passToken.getPessoa();
        pessoa.setPesSenhaLogin(passwordEncoder.encode(newPassword));
        pessoaRepository.save(pessoa);

        tokenRepository.delete(passToken);
    }
}