package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.LeadRequestDTO;
import br.com.gestaocondominio.api.domain.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/public/leads")
@CrossOrigin(origins = "*") // Permite requisições de qualquer origem (necessário para o GitHub Pages)
public class PublicLeadController {

    @Autowired
    private EmailService emailService;

    @PostMapping
    public ResponseEntity<?> receberLead(@Valid @RequestBody LeadRequestDTO dto) {
        try {
            emailService.sendLeadNotification(dto);
            return ResponseEntity.ok(Map.of("message", "Lead recebido com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Erro ao processar solicitação: " + e.getMessage()));
        }
    }
}