package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca;
import br.com.gestaocondominio.api.domain.service.FinanceiroCobrancaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cobrancas")
public class FinanceiroCobrancaController {

    private final FinanceiroCobrancaService financeiroCobrancaService;

    public FinanceiroCobrancaController(FinanceiroCobrancaService financeiroCobrancaService) {
        this.financeiroCobrancaService = financeiroCobrancaService;
    }

    @PostMapping
    public ResponseEntity<FinanceiroCobranca> cadastrarCobranca(@RequestBody FinanceiroCobranca cobranca) {
        FinanceiroCobranca novaCobranca = financeiroCobrancaService.cadastrarCobranca(cobranca);
        return new ResponseEntity<>(novaCobranca, HttpStatus.CREATED); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinanceiroCobranca> buscarCobrancaPorId(@PathVariable Integer id) {
        Optional<FinanceiroCobranca> cobranca = financeiroCobrancaService.buscarCobrancaPorId(id);
        return cobranca.map(c -> new ResponseEntity<>(c, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<FinanceiroCobranca>> listarTodasCobrancas() {
        List<FinanceiroCobranca> cobrancas = financeiroCobrancaService.listarTodasCobrancas();
        return new ResponseEntity<>(cobrancas, HttpStatus.OK); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinanceiroCobranca> atualizarCobranca(@PathVariable Integer id, @RequestBody FinanceiroCobranca cobrancaAtualizada) {
        FinanceiroCobranca cobrancaSalva = financeiroCobrancaService.atualizarCobranca(id, cobrancaAtualizada);
        return new ResponseEntity<>(cobrancaSalva, HttpStatus.OK); 
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<FinanceiroCobranca> cancelarCobranca(@PathVariable Integer id) {
        FinanceiroCobranca cobrancaCancelada = financeiroCobrancaService.cancelarCobranca(id);
        return new ResponseEntity<>(cobrancaCancelada, HttpStatus.OK);
    }

    @PostMapping("/gerar-lote")
    public ResponseEntity<List<FinanceiroCobranca>> gerarCobrancasEmLote(
            @RequestParam Integer condominioId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataVencimento,
            @RequestParam Integer tipoCobrancaId) {

        List<FinanceiroCobranca> novasCobrancas = financeiroCobrancaService.gerarCobrancasEmLote(condominioId, dataVencimento, tipoCobrancaId);
        return new ResponseEntity<>(novasCobrancas, HttpStatus.CREATED); 
    }
}