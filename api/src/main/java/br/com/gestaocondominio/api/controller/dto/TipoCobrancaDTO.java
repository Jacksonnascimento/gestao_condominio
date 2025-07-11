package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import java.math.BigDecimal;

public record TipoCobrancaDTO(
    Integer ticCod,
    String ticDescricao,
    BigDecimal ticValor,
    Boolean ticAtiva,
    Integer conCod,
    String conNome
) {
    public TipoCobrancaDTO(TipoCobranca tipoCobranca) {
        this(
            tipoCobranca.getTicCod(),
            tipoCobranca.getTicDescricao(),
            tipoCobranca.getTicValor(),
            tipoCobranca.getTicAtiva(),
            tipoCobranca.getCondominio().getConCod(),
            tipoCobranca.getCondominio().getConNome()
        );
    }
}