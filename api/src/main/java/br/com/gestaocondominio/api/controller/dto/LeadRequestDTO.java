package br.com.gestaocondominio.api.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LeadRequestDTO(
    @NotBlank(message = "O nome é obrigatório") String name,
    @NotBlank(message = "O e-mail é obrigatório") @Email(message = "E-mail inválido") String email,
    @NotBlank(message = "O WhatsApp é obrigatório") String whatsapp,
    @NotBlank(message = "O interesse é obrigatório") String reason
) {}