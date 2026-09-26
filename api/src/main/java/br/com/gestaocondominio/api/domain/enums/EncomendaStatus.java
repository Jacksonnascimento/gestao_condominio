package br.com.gestaocondominio.api.domain.enums;

import lombok.Getter;

@Getter
public enum EncomendaStatus {
    PENDENTE("Pendente"),
    RETIRADA("Retirada"),
    DEVOLVIDA("Devolvida"),
    EXTRAVIADA("Extraviada");

    private final String descricao;

    EncomendaStatus(String descricao) {
        this.descricao = descricao;
    }
}