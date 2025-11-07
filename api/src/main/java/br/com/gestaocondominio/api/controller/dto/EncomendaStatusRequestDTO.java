package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.enums.EncomendaStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EncomendaStatusRequestDTO {

    @NotNull(message = "O novo status é obrigatório.")
    private EncomendaStatus novoStatus;
    private String observacoes;
}