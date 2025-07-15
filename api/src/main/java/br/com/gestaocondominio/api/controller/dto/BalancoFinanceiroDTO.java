package br.com.gestaocondominio.api.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalancoFinanceiroDTO {
    private Integer condominioId;
    private LocalDate periodoInicio;
    private LocalDate periodoFim;
    private BigDecimal totalReceitas;
    private BigDecimal totalDespesas;
    private BigDecimal saldoFinal;
}