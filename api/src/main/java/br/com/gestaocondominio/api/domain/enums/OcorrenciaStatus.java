package br.com.gestaocondominio.api.domain.enums;

import lombok.Getter;

@Getter
public enum OcorrenciaStatus {
    ABERTA("Aberta"),
    EM_ANALISE("Em Análise"),
    RESOLVIDA("Resolvida");

    private final String descricao;

    OcorrenciaStatus(String descricao) {
        this.descricao = descricao;
    }
}