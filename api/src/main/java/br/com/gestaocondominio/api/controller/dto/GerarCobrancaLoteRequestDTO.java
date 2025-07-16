package br.com.gestaocondominio.api.controller.dto;

import java.time.LocalDate;

public class GerarCobrancaLoteRequestDTO {
    private Integer condominioId;
    private Integer tipoCobrancaId;
    private LocalDate dataVencimento;

    
    public Integer getCondominioId() {
        return condominioId;
    }
    public void setCondominioId(Integer condominioId) {
        this.condominioId = condominioId;
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
}