package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.enums.DespesaStatusPagamento; 
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DespesaRequestDTO(
    Integer conCod,
    String desDescricao,
    BigDecimal desValor,
    LocalDate desDataVencimento,
    LocalDate desDataPagamento,
    DespesaStatusPagamento desStatusPagamento, 
    Integer categoriaCod
) {}