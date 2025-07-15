package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.enums.CobrancaStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FinanceiroCobrancaRequestDTO(
    Integer undCod, 
    Integer tcoCod, 
    BigDecimal ficValorTaxa,
    LocalDate ficDtVencimento, 
    CobrancaStatus ficStatusPagamento,
    LocalDate ficDtPagamento,
    BigDecimal ficValorPago
   
) {}