package br.com.gestaocondominio.api.domain.enums;

import lombok.Getter;

@Getter
public enum EncomendaTipo {
    CORREIOS("Correios"),
    TRANSPORTADORA("Transportadora"),
    DELIVERY("Delivery"),
    OUTROS("Outros");

    private final String descricao;

    EncomendaTipo(String descricao) {
        this.descricao = descricao;
    }
}