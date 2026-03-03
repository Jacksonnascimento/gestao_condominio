package br.com.gestaocondominio.api.controller.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaRequestDTO {
    private Integer areCod;
    private Integer turCod;
    private Integer uniCod;
    private Integer pesCodMorador;
    private LocalDate data;
    private Boolean termosAceitos;
    private List<ReservaConvidadoDTO> convidados;
}