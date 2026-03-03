package br.com.gestaocondominio.api.controller.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaConvidadoDTO {
    private Integer rcvCod;
    private String nome;
    private String documento;
}