package br.com.gestaocondominio.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OcorrenciaComentarioRequestDTO {

    @NotBlank(message = "O comentário não pode estar vazio.")
    private String comentario;
}