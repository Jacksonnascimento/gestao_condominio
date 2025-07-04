package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca;
import br.com.gestaocondominio.api.domain.service.FinanceiroCobrancaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    // A permissão será validada no serviço
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
    public ResponseEntity<FinanceiroCobranca> atualizarCobranca(@PathVariable Integer id,
            @RequestBody FinanceiroCobranca cobrancaAtualizada) {
        FinanceiroCobranca cobrancaSalva = financeiroCobrancaService.atualizarCobranca(id, cobrancaAtualizada);
        return new ResponseEntity<>(cobrancaSalva, HttpStatus.OK);
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<FinanceiroCobranca> cancelarCobranca(@PathVariable Integer id) {
        FinanceiroCobranca cobrancaCancelada = financeiroCobrancaService.cancelarCobranca(id);
        return new ResponseEntity<>(cobrancaCancelada, HttpStatus.OK);
    }

    @PostMapping("/gerar-lote")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or hasAnyAuthority('ROLE_SINDICO_' + #condominioId, 'ROLE_ADMIN_' + #condominioId)")
    public ResponseEntity<List<FinanceiroCobranca>> gerarCobrancasEmLote(
            @RequestParam Integer condominioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVencimento,
            @RequestParam Integer tipoCobrancaId,

            @RequestParam(required = false) BigDecimal valor) {

        List<FinanceiroCobranca> novasCobrancas = financeiroCobrancaService.gerarCobrancasEmLote(condominioId,
                dataVencimento, tipoCobrancaId, valor);
        return new ResponseEntity<>(novasCobrancas, HttpStatus.CREATED);
    }
}