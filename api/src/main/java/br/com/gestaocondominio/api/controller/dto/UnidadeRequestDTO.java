package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.enums.UnidadeTipo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor 
public class UnidadeRequestDTO {

    private String uniNumero;
    private UnidadeStatusOcupacao uniStatusOcupacao;
    private Integer conCod;
    private UnidadeTipo unidadeTipo;
    private Boolean uniAtiva;
    private String bloco;
    private String andar;
    private BigDecimal fracaoIdeal;
    private BigDecimal areaPrivada;
    private String observacao;

    
    public UnidadeRequestDTO(Unidade unidade) {
        this.uniNumero = unidade.getUniNumero();
        this.uniStatusOcupacao = unidade.getUniStatusOcupacao();
        this.conCod = unidade.getCondominio().getConCod();
        this.unidadeTipo = unidade.getUnidadeTipo();
        this.uniAtiva = unidade.getUniAtiva();
        this.bloco = unidade.getBloco();
        this.andar = unidade.getAndar();
        this.fracaoIdeal = unidade.getFracaoIdeal();
        this.areaPrivada = unidade.getAreaPrivada();
        this.observacao = unidade.getObservacao();
    }
}