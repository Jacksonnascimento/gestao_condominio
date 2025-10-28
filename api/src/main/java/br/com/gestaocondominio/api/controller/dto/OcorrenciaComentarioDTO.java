package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.OcorrenciaComentario;

import java.time.LocalDateTime;

public record OcorrenciaComentarioDTO(
        Integer id,
        Integer ocorrenciaId,
        String comentario,
        String nomeUsuario,
        LocalDateTime dataComentario
) {
    public OcorrenciaComentarioDTO(OcorrenciaComentario comentario) {
        this(
                comentario.getOccCod(),
                comentario.getOcorrencia() != null ? comentario.getOcorrencia().getOcoCod() : null,
                comentario.getComentario(),
                comentario.getPessoaComentario() != null ? comentario.getPessoaComentario().getPesNome() : "Usuário desconhecido",
                comentario.getDataComentario()
        );
    }
}