package br.com.gestaocondominio.api.domain.enums;

public enum TipoPeriodoOcupante {
    FIXO("Fixo"),
    FLUANTE("Fluante"),
    MISTO("Misto");

    private final String descricao;

    TipoPeriodoOcupante(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}