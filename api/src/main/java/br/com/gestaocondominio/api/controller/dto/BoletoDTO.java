package br.com.gestaocondominio.api.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BoletoDTO {

    private String id;
    private String unidadeNome;
    private String nomeTaxa;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private String status;
    private String linhaDigitavel;
    private String codigoPix;
    private String linkPdf;

    public BoletoDTO() {
    }

    public BoletoDTO(String id, String unidadeNome, String nomeTaxa, BigDecimal valor, LocalDate dataVencimento, String status, String linhaDigitavel, String codigoPix, String linkPdf) {
        this.id = id;
        this.unidadeNome = unidadeNome;
        this.nomeTaxa = nomeTaxa;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.status = status;
        this.linhaDigitavel = linhaDigitavel;
        this.codigoPix = codigoPix;
        this.linkPdf = linkPdf;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUnidadeNome() {
        return unidadeNome;
    }

    public void setUnidadeNome(String unidadeNome) {
        this.unidadeNome = unidadeNome;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLinhaDigitavel() {
        return linhaDigitavel;
    }

    public void setLinhaDigitavel(String linhaDigitavel) {
        this.linhaDigitavel = linhaDigitavel;
    }

    public String getCodigoPix() {
        return codigoPix;
    }

    public void setCodigoPix(String codigoPix) {
        this.codigoPix = codigoPix;
    }

    public String getLinkPdf() {
        return linkPdf;
    }

    public void setLinkPdf(String linkPdf) {
        this.linkPdf = linkPdf;
    }
}