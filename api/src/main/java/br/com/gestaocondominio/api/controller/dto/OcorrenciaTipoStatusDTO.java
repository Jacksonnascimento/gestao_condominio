package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;

public record OcorrenciaTipoStatusDTO(String name, String descricao) {

    public static OcorrenciaTipoStatusDTO fromEnum(OcorrenciaTipo tipo) {
        return new OcorrenciaTipoStatusDTO(tipo.name(), tipo.getDescricao());
    }

    public static OcorrenciaTipoStatusDTO fromEnum(OcorrenciaStatus status) {
        return new OcorrenciaTipoStatusDTO(status.name(), status.getDescricao());
    }
}