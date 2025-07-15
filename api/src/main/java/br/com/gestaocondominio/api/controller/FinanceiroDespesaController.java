package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.FinanceiroDespesa;
import br.com.gestaocondominio.api.domain.service.FinanceiroDespesaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/despesas")
public class FinanceiroDespesaController {

    private final FinanceiroDespesaService financeiroDespesaService;

    public FinanceiroDespesaController(FinanceiroDespesaService financeiroDespesaService) {
        this.financeiroDespesaService = financeiroDespesaService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or " +
                  "@financeiroDespesaService.hasPermissionToManageFinanceByCondominioId(#despesa.condominio.conCod)")
    public ResponseEntity<FinanceiroDespesa> cadastrarDespesa(@RequestBody FinanceiroDespesa despesa) {
        FinanceiroDespesa novaDespesa = financeiroDespesaService.cadastrarDespesa(despesa);
        return new ResponseEntity<>(novaDespesa, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinanceiroDespesa> buscarDespesaPorId(@PathVariable Integer id) {
        Optional<FinanceiroDespesa> despesa = financeiroDespesaService.buscarDespesaPorId(id);
        return despesa.map(d -> new ResponseEntity<>(d, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<FinanceiroDespesa>> listarTodasDespesas(
            @RequestParam(required = false) Integer condominioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Integer categoriaId) {
        
        List<FinanceiroDespesa> despesas = financeiroDespesaService.listarTodasDespesas(condominioId, dataInicio, dataFim, categoriaId);
        return new ResponseEntity<>(despesas, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or " +
                  "@financeiroDespesaService.hasPermissionToManageFinanceByDespesaId(#id)") // CORREÇÃO AQUI: Passando o ID da despesa
    public ResponseEntity<FinanceiroDespesa> atualizarDespesa(@PathVariable Integer id, @RequestBody FinanceiroDespesa despesaAtualizada) {
        FinanceiroDespesa despesaSalva = financeiroDespesaService.atualizarDespesa(id, despesaAtualizada);
        return new ResponseEntity<>(despesaSalva, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or " +
                  "@financeiroDespesaService.hasPermissionToManageFinanceByDespesaId(#id)") 
    public ResponseEntity<Void> deletarDespesa(@PathVariable Integer id) {
        financeiroDespesaService.deletarDespesa(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}