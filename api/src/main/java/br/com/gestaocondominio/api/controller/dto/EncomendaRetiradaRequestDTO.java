package br.com.gestaocondominio.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class EncomendaRetiradaRequestDTO {

    @NotNull(message = "A data de retirada é obrigatória.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataRetirada;

    @NotNull(message = "A hora da retirada é obrigatória.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horaRetirada;

    @NotBlank(message = "O nome de quem retirou é obrigatório.")
    private String nomeRetirada;
}