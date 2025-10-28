package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.OcorrenciaAnexo;

import java.time.LocalDateTime;

public record OcorrenciaAnexoDTO(
        Integer id,
        Integer ocorrenciaId,
        String nomeOriginal,
        String tipoArquivo,
        Long tamanhoArquivo,
        String nomeUsuario,
        LocalDateTime dataAnexo,
        String caminhoArquivo 
) {
    public OcorrenciaAnexoDTO(OcorrenciaAnexo anexo) {
        this(
                anexo.getOcaCod(),
                anexo.getOcorrencia() != null ? anexo.getOcorrencia().getOcoCod() : null,
                anexo.getNomeOriginal(),
                anexo.getTipoArquivo(),
                anexo.getTamanhoArquivo(),
                anexo.getPessoaAnexo() != null ? anexo.getPessoaAnexo().getPesNome() : "Usuário desconhecido",
                anexo.getDataAnexo(),
                anexo.getCaminhoArquivo() 
        );
    }
}