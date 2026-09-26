package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.controller.dto.ContratoRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Contrato;
import br.com.gestaocondominio.api.domain.enums.StatusContrato;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Pedidos e respostas de {@code /api/v1/contratos}. */
public final class ContratoDTOs {

    private ContratoDTOs() {
    }

    /** Abas da tela de contratos. Cada uma mostra contratos de situações diferentes. */
    public enum AbaContrato {
        ATIVOS("Contratos Ativos"),
        A_VENCER("Próximos a Vencer"),
        HISTORICO("Histórico");

        private final String descricao;

        AbaContrato(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    /**
     * Cadastro e edição. A situação é calculada pela data de fim (ativo, a vencer ou finalizado); só
     * {@code RESCINDIDO} é gravado como escolhido. O condomínio só vale no cadastro: na edição ele não muda.
     */
    public record ContratoRequest(
            Integer condominioId,
            @NotBlank(message = "Informe a empresa.")
            @Size(max = 100, message = "A empresa pode ter até 100 caracteres.") String empresa,
            @NotBlank(message = "Informe o serviço.")
            @Size(max = 255, message = "O serviço pode ter até 255 caracteres.") String servico,
            @NotNull(message = "Informe o valor.")
            @DecimalMin(value = "0.00", message = "O valor não pode ser negativo.")
            @Digits(integer = 8, fraction = 2, message = "Valor inválido.") BigDecimal valor,
            @Size(max = 100, message = "O responsável pode ter até 100 caracteres.") String responsavel,
            StatusContrato status,
            @NotNull(message = "Informe a data de início.") LocalDate dataInicio,
            @NotNull(message = "Informe a data de fim.") LocalDate dataFim,
            String observacoes) {

        public ContratoRequestDTO paraDTO() {
            ContratoRequestDTO dto = new ContratoRequestDTO();
            dto.setCondominioId(condominioId);
            dto.setEmpresa(empresa.trim());
            dto.setServico(servico.trim());
            dto.setValor(valor);
            dto.setResponsavel(responsavel);
            dto.setStatus(status);
            dto.setDataInicio(dataInicio);
            dto.setDataFim(dataFim);
            dto.setObservacoes(observacoes);
            return dto;
        }
    }

    public record ContratoResponse(Long id, Integer condominioCodigo, String condominioNome, String empresa,
                                   String servico, BigDecimal valor, String responsavel, StatusContrato status,
                                   String statusDescricao, LocalDate dataInicio, LocalDate dataFim,
                                   String observacoes, LocalDateTime dataCadastro, LocalDateTime dataAtualizacao) {

        public static ContratoResponse de(Contrato contrato) {
            Condominio condominio = contrato.getCondominio();
            return new ContratoResponse(contrato.getId(),
                    condominio == null ? null : condominio.getConCod(),
                    condominio == null ? null : condominio.getConNome(),
                    contrato.getEmpresa(), contrato.getServico(), contrato.getValor(), contrato.getResponsavel(),
                    contrato.getStatus(), contrato.getStatus() == null ? null : contrato.getStatus().getDescricao(),
                    contrato.getDataInicio(), contrato.getDataFim(), contrato.getObservacoes(),
                    contrato.getDataCadastro(), contrato.getDataAtualizacao());
        }
    }

    /** Os números dos cartões do topo da tela. */
    public record TotaisContrato(long total, long ativos, long aVencer, long finalizados, long rescindidos) {

        public static TotaisContrato de(Map<StatusContrato, Long> porStatus) {
            return new TotaisContrato(
                    porStatus.values().stream().mapToLong(Long::longValue).sum(),
                    porStatus.getOrDefault(StatusContrato.ATIVO, 0L),
                    porStatus.getOrDefault(StatusContrato.A_VENCER, 0L),
                    porStatus.getOrDefault(StatusContrato.FINALIZADO, 0L),
                    porStatus.getOrDefault(StatusContrato.RESCINDIDO, 0L));
        }
    }

    public record CondominioDisponivel(Integer codigo, String nome) {

        public static CondominioDisponivel de(Condominio condominio) {
            return new CondominioDisponivel(condominio.getConCod(), condominio.getConNome());
        }
    }

    /**
     * Listas de escolha da tela: {@code abas} (o filtro da listagem), {@code status} (todas as situações, para os
     * selos), {@code statusDoHistorico} (o filtro da aba de histórico) e {@code statusDeCadastro} (o campo do
     * formulário).
     */
    public record OpcoesContrato(List<CondominioDisponivel> condominios, List<Opcao> abas, List<Opcao> status,
                                 List<Opcao> statusDoHistorico, List<Opcao> statusDeCadastro,
                                 boolean podeGerenciar) {
    }
}
