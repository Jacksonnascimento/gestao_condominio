package br.com.gestaocondominio.api.controller.dto;

import lombok.*;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaComumTurnoDTO {
    private Integer turCod;
    private String nome;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private Boolean ativo;
}