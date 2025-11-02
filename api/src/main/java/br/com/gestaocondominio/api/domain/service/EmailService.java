package br.com.gestaocondominio.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.properties.mail.smtp.from-name}")
    private String fromName;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String fromEmail;

    @Async
    public void sendPasswordResetEmail(String to, String tokenUrl, String nomeUsuario) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = String.format("""
                <div style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2>Olá, %s!</h2>
                    <p>Recebemos uma solicitação para redefinir sua senha no sistema CONDIGTAL.</p>
                    <p>Por favor, clique no link abaixo para criar uma nova senha. Este link é válido por um tempo limitado.</p>
                    <p style="text-align: center; margin: 25px 0;">
                        <a href="%s" style="background-color: #3b82f6; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;">
                            Definir Nova Senha
                        </a>
                    </p>
                    <p>Se você não solicitou esta alteração, por favor, ignore este e-mail.</p>
                    <hr>
                    <p style="font-size: 0.9em; color: #777;">Atenciosamente,<br>Equipe CONDIGTAL</p>
                </div>
            """, nomeUsuario, tokenUrl);

            helper.setText(htmlMsg, true);
            helper.setTo(to);
            helper.setSubject("CONDIGTAL - Redefinição de Senha");
            helper.setFrom(fromEmail, fromName);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            // Logar o erro
            System.err.println("Erro ao enviar e-mail de reset de senha: " + e.getMessage());
        }
    }
}