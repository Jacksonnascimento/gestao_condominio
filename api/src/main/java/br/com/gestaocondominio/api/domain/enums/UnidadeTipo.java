package br.com.gestaocondominio.api.domain.enums;

public enum UnidadeTipo {
    APARTAMENTO("Apartamento"),
    COMERCIAL("Comercial"),
    GARAGEM("Garagem"),
    DEPOSITO("Depósito"),
    OUTROS("Outros");

    private final String descricao;

    UnidadeTipo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}