package br.com.gestaocondominio.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OcorrenciaFinalizarRequestDTO {

    @NotBlank(message = "O parecer final é obrigatório.")
    private String parecerFinal;
}