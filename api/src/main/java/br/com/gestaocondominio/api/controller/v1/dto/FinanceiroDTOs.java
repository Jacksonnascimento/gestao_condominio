package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.controller.dto.BoletoDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Pedidos e respostas de {@code /api/v1/financeiro}. O financeiro ainda é demonstração: os boletos são gerados na
 * hora, com valores e códigos fictícios, e nada é cobrado de verdade.
 */
public final class FinanceiroDTOs {

    private FinanceiroDTOs() {
    }

    /** Boleto avulso para uma unidade. Sem vencimento, vale o último dia do mês. */
    public record BoletoAvulsoRequest(
            @NotNull(message = "Escolha a unidade pagadora.") Integer unidadeId,
            @NotBlank(message = "Informe a descrição.")
            @Size(max = 255, message = "A descrição pode ter até 255 caracteres.") String nomeTaxa,
            @NotNull(message = "Informe o valor.")
            @DecimalMin(value = "0.00", message = "O valor não pode ser negativo.")
            @Digits(integer = 8, fraction = 2, message = "Valor inválido.") BigDecimal valor,
            LocalDate dataVencimento) {
    }

    /** Dados do condomínio que aparecem no boleto impresso. */
    public record CondominioDoBoleto(Integer codigo, String nome, String logradouro, String numero) {

        public static CondominioDoBoleto de(Condominio condominio) {
            return new CondominioDoBoleto(condominio.getConCod(), condominio.getConNome(),
                    condominio.getConLogradouro(), condominio.getConNumero());
        }
    }

    /**
     * O que a tela mostra: as abas "Em Aberto", "Vencidos" e "Histórico". {@code demonstracao} é sempre
     * {@code true} enquanto o financeiro for simulado.
     */
    public record PainelFinanceiro(boolean demonstracao, CondominioDoBoleto condominio, boolean podeGerarBoleto,
                                   List<BoletoDTO> boletosAbertos, List<BoletoDTO> boletosVencidos,
                                   List<BoletoDTO> historico) {
    }

    public record CondominioDisponivel(Integer codigo, String nome) {

        public static CondominioDisponivel de(Condominio condominio) {
            return new CondominioDisponivel(condominio.getConCod(), condominio.getConNome());
        }
    }

    /** Unidade pagadora, na escolha do boleto avulso. */
    public record UnidadePagadora(Integer codigo, String descricao) {

        public static UnidadePagadora de(Unidade unidade) {
            String descricao = "Unidade " + unidade.getUniNumero()
                    + (unidade.getBloco() != null && !unidade.getBloco().isBlank() ? ", Bloco " + unidade.getBloco() : "");
            return new UnidadePagadora(unidade.getUniCod(), descricao);
        }
    }

    /** {@code unidades} só vem preenchida para quem pode gerar boleto avulso no condomínio. */
    public record OpcoesFinanceiro(List<CondominioDisponivel> condominios, Integer condominioId,
                                   List<UnidadePagadora> unidades, boolean podeGerarBoleto) {
    }
}
