package br.com.gestaocondominio.api.controller.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaComumRequestDTO {
    private Integer areCod;
    private Integer conCod;
    private String nome;
    private String descricao;
    private String termosUso;
    private Integer capacidadeMaxima;
    private Boolean permiteConvidados;
    private Integer limiteConvidados;
    private Integer diasAntecedenciaMin;
    private Integer diasAntecedenciaMax;
    private Boolean ativa;
    private BigDecimal taxaValor;
    private List<AreaComumTurnoDTO> turnos;
}