package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.LeadRequestDTO;
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
            System.err.println("Erro ao enviar e-mail de reset de senha: " + e.getMessage());
        }
    }

    @Async
    public void sendLeadNotification(LeadRequestDTO lead) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // Lista de destinatários
            String[] recipients = {
                "contato@condigtal.com.br",
                "jacksonsantos2018@gmail.com",
                "andersonspita87@gmail.com"
            };

            String htmlMsg = String.format("""
                <div style="font-family: Arial, sans-serif; border: 1px solid #eee; padding: 20px; max-width: 600px;">
                    <h2 style="color: #3b82f6; border-bottom: 2px solid #3b82f6; padding-bottom: 10px;">Novo Lead - CONDIGTAL</h2>
                    <p style="font-size: 1.1em;">Um novo interessado preencheu o formulário na Landing Page.</p>
                    <table style="width: 100%%; border-collapse: collapse; margin-top: 20px;">
                        <tr style="background-color: #f9f9f9;">
                            <td style="padding: 10px; font-weight: bold; width: 30%%;">Nome:</td>
                            <td style="padding: 10px;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; font-weight: bold;">E-mail:</td>
                            <td style="padding: 10px;">%s</td>
                        </tr>
                        <tr style="background-color: #f9f9f9;">
                            <td style="padding: 10px; font-weight: bold;">WhatsApp:</td>
                            <td style="padding: 10px;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; font-weight: bold;">Interesse:</td>
                            <td style="padding: 10px;">%s</td>
                        </tr>
                    </table>
                    <hr style="margin-top: 30px; border: 0; border-top: 1px solid #eee;">
                    <p style="font-size: 0.8em; color: #999; text-align: center;">Enviado automaticamente pelo sistema CONDIGTAL.</p>
                </div>
            """, lead.name(), lead.email(), lead.whatsapp(), lead.reason());

            helper.setText(htmlMsg, true);
            helper.setTo(recipients); // Envia para todos os e-mails
            helper.setSubject("NOVO LEAD: " + lead.name() + " - Interesse em " + lead.reason());
            helper.setFrom(fromEmail, "CONDIGTAL Landing Page");

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            System.err.println("Erro ao enviar notificação de lead: " + e.getMessage());
        }
    }
}