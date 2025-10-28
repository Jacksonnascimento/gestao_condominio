package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Ocorrencia;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record OcorrenciaDetalhesDTO(
        Integer id,
        String titulo,
        String descricaoCompleta,
        OcorrenciaStatus status,
        OcorrenciaTipo tipo,
        String unidadeNumero,
        String unidadeBloco,
        String condominioNome,
        LocalDateTime dataRegistro,
        String nomePessoaRegistro,
        String parecerFinal,
        String nomePessoaFinalizou,
        LocalDateTime dataFinalizacao,
        List<OcorrenciaComentarioDTO> comentarios,
        List<OcorrenciaAnexoDTO> anexos
) {
    public static OcorrenciaDetalhesDTO fromEntity(Ocorrencia ocorrencia) {
        return new OcorrenciaDetalhesDTO(
                ocorrencia.getOcoCod(),
                ocorrencia.getTitulo(),
                ocorrencia.getDescricao(),
                ocorrencia.getStatus(),
                ocorrencia.getTipo(),
                ocorrencia.getUnidade() != null ? ocorrencia.getUnidade().getUniNumero() : null,
                ocorrencia.getUnidade() != null ? ocorrencia.getUnidade().getBloco() : null,
                ocorrencia.getCondominio() != null ? ocorrencia.getCondominio().getConNome() : null,
                ocorrencia.getDataRegistro(),
                ocorrencia.getPessoaRegistro() != null ? ocorrencia.getPessoaRegistro().getPesNome() : "Desconhecido",
                ocorrencia.getParecerFinal(),
                ocorrencia.getPessoaFinalizou() != null ? ocorrencia.getPessoaFinalizou().getPesNome() : null,
                ocorrencia.getDataFinalizacao(),
                ocorrencia.getComentarios() != null ? ocorrencia.getComentarios().stream().map(OcorrenciaComentarioDTO::new).collect(Collectors.toList()) : List.of(),
                ocorrencia.getAnexos() != null ? ocorrencia.getAnexos().stream().map(OcorrenciaAnexoDTO::new).collect(Collectors.toList()) : List.of()
        );
    }
}