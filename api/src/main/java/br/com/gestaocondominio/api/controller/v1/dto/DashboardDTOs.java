package br.com.gestaocondominio.api.controller.v1.dto;

/** Respostas de {@code /api/v1/dashboard}. */
public final class DashboardDTOs {

    private DashboardDTOs() {
    }

    /**
     * Indicadores do painel. Cada um conta só o que a pessoa veria no módulo correspondente; vem nulo quando o
     * módulo não é dela (contratos, para quem é só da portaria), e a tela esconde o cartão.
     *
     * @param totalUnidades             unidades ativas
     * @param totalOcupantes            ocupantes cadastrados nas unidades
     * @param totalContratosAtivos      contratos na situação "Ativo" (não rescindidos e com mais de 30 dias até o fim)
     * @param totalOcorrenciasPendentes ocorrências abertas ou em análise
     */
    public record Painel(Long totalUnidades, Long totalOcupantes, Long totalContratosAtivos,
                         Long totalOcorrenciasPendentes) {
    }
}
