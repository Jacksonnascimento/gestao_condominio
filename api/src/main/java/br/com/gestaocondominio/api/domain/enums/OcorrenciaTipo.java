package br.com.gestaocondominio.api.domain.enums;

import lombok.Getter;

@Getter
public enum OcorrenciaTipo {
    BARULHO("Barulho"),
    CONFLITO("Conflito"),
    MANUTENCAO("Manutenção"),
    RECLAMACAO("Reclamação"),
    OUTRO("Outro");

    private final String descricao;

    OcorrenciaTipo(String descricao) {
        this.descricao = descricao;
    }
}