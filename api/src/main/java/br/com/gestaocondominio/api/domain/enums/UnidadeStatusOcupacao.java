package br.com.gestaocondominio.api.domain.enums;

public enum UnidadeStatusOcupacao {
    OCUPADA("Ocupada"),
    VAZIA("Vazia"),
    EM_REFORMA("Em Reforma"),
    MULTIPROPRIEDADE("Multipropriedade");

    private final String descricao;

    UnidadeStatusOcupacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}