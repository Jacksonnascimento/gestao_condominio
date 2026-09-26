package br.com.gestaocondominio.api.domain.enums;

import lombok.Getter;

@Getter
public enum VisitanteStatus {
    NO_LOCAL("No local"),
    SAIU("Saiu");

    private final String descricao;

    VisitanteStatus(String descricao) {
        this.descricao = descricao;
    }
}