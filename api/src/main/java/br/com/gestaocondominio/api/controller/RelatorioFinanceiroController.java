package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.BalancoFinanceiroDTO;
import br.com.gestaocondominio.api.domain.service.RelatorioFinanceiroService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/financeiro/relatorios")
public class RelatorioFinanceiroController {

    private final RelatorioFinanceiroService relatorioFinanceiroService;

    public RelatorioFinanceiroController(RelatorioFinanceiroService relatorioFinanceiroService) {
        this.relatorioFinanceiroService = relatorioFinanceiroService;
    }

    @GetMapping("/balanco")
    public ResponseEntity<BalancoFinanceiroDTO> gerarBalancoFinanceiro(
            @RequestParam Integer condominioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        
        BalancoFinanceiroDTO balanco = relatorioFinanceiroService.gerarBalançoFinanceiro(condominioId, dataInicio, dataFim);
        return new ResponseEntity<>(balanco, HttpStatus.OK);
    }
}