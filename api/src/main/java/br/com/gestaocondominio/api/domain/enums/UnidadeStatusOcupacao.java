package br.com.gestaocondominio.api.domain.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
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