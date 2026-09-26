package br.com.gestaocondominio.api.domain.enums;

public enum OcupanteVinculo {
    PROPRIETARIO("Proprietário"),
    LOCATARIO("Locatário"),
    PROMITENTE_COMPRADOR("Promitente comprador"),
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