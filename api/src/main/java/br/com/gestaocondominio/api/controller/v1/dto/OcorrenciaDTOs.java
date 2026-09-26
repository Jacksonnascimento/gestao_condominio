package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.controller.dto.OcorrenciaAnexoDTO;
import br.com.gestaocondominio.api.controller.dto.OcorrenciaComentarioDTO;
import br.com.gestaocondominio.api.controller.dto.OcorrenciaDetalhesDTO;
import br.com.gestaocondominio.api.controller.dto.OcorrenciaRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** Pedidos e respostas de {@code /api/v1/ocorrencias}. */
public final class OcorrenciaDTOs {

    private OcorrenciaDTOs() {
    }

    /** Registro de ocorrência. O {@code condominioId} só é pedido ao administrador geral. */
    public record OcorrenciaRequest(
            @NotNull(message = "A unidade é obrigatória.") Integer unidadeId,
            @NotNull(message = "O tipo da ocorrência é obrigatório.") OcorrenciaTipo tipo,
            @NotBlank(message = "O título é obrigatório.")
            @Size(max = 150, message = "O título não pode exceder 150 caracteres.") String titulo,
            @NotBlank(message = "A descrição é obrigatória.") String descricao,
            Integer condominioId) {

        public OcorrenciaRequestDTO paraDTO() {
            OcorrenciaRequestDTO dto = new OcorrenciaRequestDTO();
            dto.setUnidadeId(unidadeId);
            dto.setTipo(tipo);
            dto.setTitulo(titulo.trim());
            dto.setDescricao(descricao);
            dto.setCondominioId(condominioId);
            return dto;
        }
    }

    /**
     * Uma ocorrência com o histórico. Como na tela, comentários e anexos só vêm para quem gerencia as ocorrências do
     * condomínio ({@code podeGerenciar}); para os demais as listas vêm vazias. O nome de quem registrou vem como
     * "Morador" quando a pessoa não pode saber quem foi.
     */
    public record OcorrenciaDetalhe(Integer id, String titulo, String descricao, OcorrenciaStatus status,
                                    OcorrenciaTipo tipo, String unidadeNumero, String unidadeBloco,
                                    String condominioNome, LocalDateTime dataRegistro, String nomePessoaRegistro,
                                    String parecerFinal, String nomePessoaFinalizou, LocalDateTime dataFinalizacao,
                                    List<OcorrenciaComentarioDTO> comentarios, List<Anexo> anexos,
                                    boolean podeGerenciar) {

        public static OcorrenciaDetalhe de(OcorrenciaDetalhesDTO detalhes, boolean podeGerenciar) {
            return new OcorrenciaDetalhe(detalhes.id(), detalhes.titulo(), detalhes.descricaoCompleta(),
                    detalhes.status(), detalhes.tipo(), detalhes.unidadeNumero(), detalhes.unidadeBloco(),
                    detalhes.condominioNome(), detalhes.dataRegistro(), detalhes.nomePessoaRegistro(),
                    detalhes.parecerFinal(), detalhes.nomePessoaFinalizou(), detalhes.dataFinalizacao(),
                    podeGerenciar ? detalhes.comentarios() : List.of(),
                    podeGerenciar ? detalhes.anexos().stream().map(Anexo::de).toList() : List.of(),
                    podeGerenciar);
        }
    }

    /** Anexo da ocorrência, sem o caminho em que o arquivo fica guardado no servidor. */
    public record Anexo(Integer id, String nomeOriginal, String tipoArquivo, Long tamanhoArquivo, String nomeUsuario,
                        LocalDateTime dataAnexo) {

        public static Anexo de(OcorrenciaAnexoDTO anexo) {
            return new Anexo(anexo.id(), anexo.nomeOriginal(), anexo.tipoArquivo(), anexo.tamanhoArquivo(),
                    anexo.nomeUsuario(), anexo.dataAnexo());
        }
    }

    public record CondominioResumo(Integer codigo, String nome) {

        public static CondominioResumo de(Condominio condominio) {
            return new CondominioResumo(condominio.getConCod(), condominio.getConNome());
        }
    }

    public record UnidadeResumo(Integer codigo, String numero, String bloco) {

        public static UnidadeResumo de(Unidade unidade) {
            return new UnidadeResumo(unidade.getUniCod(), unidade.getUniNumero(), unidade.getBloco());
        }
    }
}
