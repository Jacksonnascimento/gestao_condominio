package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.enums.UnidadeTipo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class UnidadeRequestDTO {

    @NotNull
    private String uniNumero;
    
    @NotNull
    private UnidadeStatusOcupacao uniStatusOcupacao;
    
    @NotNull
    private Integer conCod;
    
    @NotNull
    private UnidadeTipo unidadeTipo;
    
    private Boolean uniAtiva;
    private String bloco;
    private String andar;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "A fração ideal não pode ser menor que 0.")
    @DecimalMax(value = "100.0", inclusive = true, message = "A fração ideal não pode ser maior que 100.")
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