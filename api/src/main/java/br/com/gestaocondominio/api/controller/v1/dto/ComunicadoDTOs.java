package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.controller.dto.ComunicadoRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Comunicado;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Pedidos e respostas de {@code /api/v1/comunicados}. */
public final class ComunicadoDTOs {

    private ComunicadoDTOs() {
    }

    /**
     * Dados do comunicado, na parte {@code comunicado} (JSON) do formulário. Os {@code condominioIds} só valem para o
     * administrador geral; os demais publicam sempre no próprio condomínio.
     */
    public record ComunicadoRequest(
            @NotBlank(message = "Informe o título.") @Size(max = 255, message = "O título pode ter até 255 caracteres.")
            String titulo,
            @NotBlank(message = "Informe a mensagem.") String mensagem,
            @NotNull(message = "Informe o público do comunicado.") PublicoDestino publicoDestino,
            Boolean urgente,
            List<Integer> condominioIds) {

        public ComunicadoRequestDTO paraDTO() {
            ComunicadoRequestDTO dto = new ComunicadoRequestDTO();
            dto.setTitulo(titulo.trim());
            dto.setMensagem(mensagem);
            dto.setPublicoDestino(publicoDestino);
            dto.setIsUrgente(Boolean.TRUE.equals(urgente));
            dto.setCondominioIds(condominioIds);
            return dto;
        }
    }

    /**
     * Um comunicado como a tela mostra. Os condomínios de destino só vêm para o administrador geral (os demais só veem
     * os do próprio condomínio). {@code podeGerenciar} diz se quem está logado pode editar e excluir este comunicado.
     */
    public record ComunicadoResposta(Integer id, String titulo, String mensagem, PublicoDestino publicoDestino,
                                     String publicoDestinoDescricao, boolean urgente, LocalDateTime dataCadastro,
                                     String autor, boolean possuiAnexo, String nomeAnexo,
                                     List<CondominioResumo> condominios, boolean podeGerenciar) {

        public static ComunicadoResposta de(Comunicado comunicado, boolean mostrarCondominios, boolean podeGerenciar) {
            String anexo = comunicado.getCaminhoAnexo();
            boolean possuiAnexo = anexo != null && !anexo.isBlank();
            return new ComunicadoResposta(comunicado.getComId(), comunicado.getTitulo(), comunicado.getMensagem(),
                    comunicado.getPublicoDestino(), comunicado.getPublicoDestino().getDescricao(),
                    Boolean.TRUE.equals(comunicado.getIsUrgente()), comunicado.getDataCadastro(),
                    comunicado.getCriador() == null ? null : comunicado.getCriador().getPesNome(),
                    possuiAnexo, possuiAnexo ? nomeDoAnexo(comunicado) : null,
                    mostrarCondominios && comunicado.getCondominios() != null
                            ? comunicado.getCondominios().stream()
                                    .map(CondominioResumo::de)
                                    .sorted(Comparator.comparing(CondominioResumo::nome,
                                            Comparator.nullsLast(String::compareToIgnoreCase)))
                                    .toList()
                            : List.of(),
                    podeGerenciar);
        }
    }

    /**
     * Nome com que o anexo foi enviado. Nos comunicados antigos, que não o guardaram, "anexo" com a extensão do
     * arquivo, em vez do nome gerado com que ele foi guardado.
     */
    public static String nomeDoAnexo(Comunicado comunicado) {
        String nome = comunicado.getNomeAnexo();
        if (nome != null && !nome.isBlank()) {
            return nome;
        }
        String guardado = Paths.get(comunicado.getCaminhoAnexo()).getFileName().toString();
        int ponto = guardado.lastIndexOf('.');
        return ponto > 0 ? "anexo" + guardado.substring(ponto) : "anexo";
    }

    public record CondominioResumo(Integer codigo, String nome) {

        public static CondominioResumo de(Condominio condominio) {
            return new CondominioResumo(condominio.getConCod(), condominio.getConNome());
        }
    }
}
