package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import br.com.gestaocondominio.api.domain.enums.UnidadeTipo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Pedidos e respostas de {@code /api/v1/unidades} e da lista de unidades de um condomínio. */
public final class UnidadeDTOs {

    private UnidadeDTOs() {
    }

    /**
     * Dados do cadastro. O {@code condominioId} só é obrigatório para quem gerencia mais de um condomínio; na edição
     * ele é ignorado, porque a unidade não muda de condomínio.
     */
    public record UnidadeRequest(
            Integer condominioId,
            @NotBlank(message = "Informe o número da unidade.")
            @Size(max = 10, message = "O número pode ter até 10 caracteres.") String numero,
            @Size(max = 50, message = "O bloco pode ter até 50 caracteres.") String bloco,
            @Size(max = 50, message = "O andar pode ter até 50 caracteres.") String andar,
            @NotNull(message = "Informe o tipo da unidade.") UnidadeTipo tipo,
            @NotNull(message = "Informe a situação de ocupação.") UnidadeStatusOcupacao statusOcupacao,
            @DecimalMin(value = "0.0", message = "A fração ideal não pode ser menor que 0.")
            @DecimalMax(value = "100.0", message = "A fração ideal não pode ser maior que 100.") BigDecimal fracaoIdeal,
            @DecimalMin(value = "0.0", message = "A área privada não pode ser negativa.")
            @Digits(integer = 8, fraction = 2, message = "Área privada inválida.") BigDecimal areaPrivada,
            String observacao) {

        public UnidadeRequestDTO paraDto() {
            UnidadeRequestDTO dto = new UnidadeRequestDTO();
            dto.setConCod(condominioId);
            dto.setUniNumero(numero.trim());
            dto.setBloco(vazioComoNulo(bloco));
            dto.setAndar(vazioComoNulo(andar));
            dto.setUnidadeTipo(tipo);
            dto.setUniStatusOcupacao(statusOcupacao);
            dto.setFracaoIdeal(fracaoIdeal);
            dto.setAreaPrivada(areaPrivada);
            dto.setObservacao(observacao);
            return dto;
        }

        private static String vazioComoNulo(String valor) {
            return valor == null || valor.isBlank() ? null : valor.trim();
        }
    }

    public record UnidadeResposta(Integer id, Integer condominioId, String condominioNome, String numero, String bloco,
                                  String andar, String tipo, String tipoDescricao,
                                  String statusOcupacao, String statusOcupacaoDescricao,
                                  BigDecimal fracaoIdeal, BigDecimal areaPrivada, String observacao, boolean ativa,
                                  LocalDateTime dataCadastro, LocalDateTime dataAtualizacao) {

        public static UnidadeResposta de(Unidade u) {
            return new UnidadeResposta(u.getUniCod(), u.getCondominio().getConCod(), u.getCondominio().getConNome(),
                    u.getUniNumero(), u.getBloco(), u.getAndar(), nome(u.getUnidadeTipo()),
                    u.getUnidadeTipo() == null ? null : u.getUnidadeTipo().getDescricao(), nome(u.getUniStatusOcupacao()),
                    u.getUniStatusOcupacao() == null ? null : u.getUniStatusOcupacao().getDescricao(),
                    u.getFracaoIdeal(), u.getAreaPrivada(), u.getObservacao(), Boolean.TRUE.equals(u.getUniAtiva()),
                    u.getUniDtCadastro(), u.getUniDtAtualizacao());
        }
    }

    /** Unidade nas listas de escolha dos formulários: só o que identifica a unidade. */
    public record UnidadeResumo(Integer id, String numero, String bloco, String andar, String tipo,
                                String tipoDescricao) {

        public static UnidadeResumo de(Unidade u) {
            return new UnidadeResumo(u.getUniCod(), u.getUniNumero(), u.getBloco(), u.getAndar(), nome(u.getUnidadeTipo()),
                    u.getUnidadeTipo() == null ? null : u.getUnidadeTipo().getDescricao());
        }
    }

    /**
     * Os enums de unidade saem como objeto no JSON das telas antigas ({@code @JsonFormat(OBJECT)}); aqui vão pelo
     * nome, que é o valor aceito nos pedidos, com a descrição num campo à parte.
     */
    private static String nome(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }

    public record OpcoesUnidade(List<Opcao> tipos, List<Opcao> statusOcupacao, boolean podeGerenciar) {
    }

    /**
     * Resposta 409 do cadastro quando a unidade já existiu e foi inativada: a tela oferece reativar a
     * {@code unidadeId} em vez de criar outra.
     */
    public record UnidadeInativaResposta(LocalDateTime timestamp, int status, String error, String message,
                                         String path, Integer unidadeId) {
    }
}
