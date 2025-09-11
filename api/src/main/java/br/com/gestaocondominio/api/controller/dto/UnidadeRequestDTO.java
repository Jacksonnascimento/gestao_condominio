package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UnidadeRequestDTO {

    private String uniNumero;
    private UnidadeStatusOcupacao uniStatusOcupacao;
    private Integer conCod;
    private Integer utiCod;
    private Boolean uniAtiva;
    private String bloco;
    private String andar;
    private BigDecimal fracaoIdeal;
    private BigDecimal areaPrivada;
    private String observacao;

}