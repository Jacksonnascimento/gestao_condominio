package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Ocorrencia;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;

import java.time.LocalDateTime;

public record OcorrenciaResumoDTO(
        Integer id,
        String titulo,
        OcorrenciaStatus status,
        OcorrenciaTipo tipo,
        String unidadeNumero,
        String unidadeBloco,
        String condominioNome, 
        LocalDateTime dataRegistro,
        String descricaoCurta 
) {
    public OcorrenciaResumoDTO(Ocorrencia ocorrencia) {
        this(
                ocorrencia.getOcoCod(),
                ocorrencia.getTitulo(),
                ocorrencia.getStatus(),
                ocorrencia.getTipo(),
                ocorrencia.getUnidade() != null ? ocorrencia.getUnidade().getUniNumero() : null,
                ocorrencia.getUnidade() != null ? ocorrencia.getUnidade().getBloco() : null,
                ocorrencia.getCondominio() != null ? ocorrencia.getCondominio().getConNome() : null,
                ocorrencia.getDataRegistro(),
                ocorrencia.getDescricao() != null && ocorrencia.getDescricao().length() > 100 ?
                        ocorrencia.getDescricao().substring(0, 100) + "..." : ocorrencia.getDescricao()
        );
    }
}