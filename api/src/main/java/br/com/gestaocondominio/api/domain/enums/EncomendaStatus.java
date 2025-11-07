package br.com.gestaocondominio.api.domain.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EncomendaStatus {
    PENDENTE("Pendente"),
    RETIRADA("Retirada"),
    DEVOLVIDA("Devolvida"),
    EXTRAVIADA("Extraviada");

    private final String descricao;

    EncomendaStatus(String descricao) {
        this.descricao = descricao;
    }

    public String getNome() {
        return this.name();
    }
}