package br.com.gestaocondominio.api.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GerarCobrancaIndividualRequestDTO {
    private Integer unidadeId;
    private Integer tipoCobrancaId;
    private LocalDate dataVencimento;
    private BigDecimal valorOpcional;

    
    public Integer getUnidadeId() {
        return unidadeId;
    }
    public void setUnidadeId(Integer unidadeId) {
        this.unidadeId = unidadeId;
    }
    public Integer getTipoCobrancaId() {
        return tipoCobrancaId;
    }
    public void setTipoCobrancaId(Integer tipoCobrancaId) {
        this.tipoCobrancaId = tipoCobrancaId;
    }
    public LocalDate getDataVencimento() {
        return dataVencimento;
    }
    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }
    public BigDecimal getValorOpcional() {
        return valorOpcional;
    }
    public void setValorOpcional(BigDecimal valorOpcional) {
        this.valorOpcional = valorOpcional;
    }
}