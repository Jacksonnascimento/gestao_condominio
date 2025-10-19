package br.com.gestaocondominio.api.domain.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum OcupanteVinculo {
    PROPRIETARIO("Proprietário"),
    LOCATARIO("Locatário"),
    PROMITENTE_COMPRADOR("Promitente Comprador"),
    CESSIONARIO("Cessionário"),
    MULTIPROPRIETARIO("Multiproprietário"),
    CONJUGE("Cônjuge"),
    DEPENDENTE("Dependente");

    private final String descricao;

    OcupanteVinculo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}