package br.com.gestaocondominio.api.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BoletoGeracaoRequestDTO {

    private Integer unidadeId; 
    private String nomeTaxa;
    private BigDecimal valor;
    private LocalDate dataVencimento;

    public Integer getUnidadeId() {
        return unidadeId;
    }

    public void setUnidadeId(Integer unidadeId) {
        this.unidadeId = unidadeId;
    }

    public String getNomeTaxa() {
        return nomeTaxa;
    }

    public void setNomeTaxa(String nomeTaxa) {
        this.nomeTaxa = nomeTaxa;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }
}