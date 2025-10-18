package br.com.gestaocondominio.api.domain.enums;

public enum StatusContrato {
    ATIVO("Ativo"),
    A_VENCER("A Vencer"),
    FINALIZADO("Finalizado"),
    RESCINDIDO("Rescindido");

    private final String descricao;

    StatusContrato(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}