package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.FinanceiroCobrancaRequestDTO;
import br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca;
import br.com.gestaocondominio.api.domain.service.FinanceiroCobrancaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat; 

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
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or " +
                  "@financeiroCobrancaService.hasPermissionToManageCobrancaByUnidadeId(#requestDTO.undCod)")
    public ResponseEntity<FinanceiroCobranca> cadastrarCobranca(@RequestBody FinanceiroCobrancaRequestDTO requestDTO) {
        FinanceiroCobranca novaCobranca = financeiroCobrancaService.cadastrarCobranca(requestDTO);
        return new ResponseEntity<>(novaCobranca, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinanceiroCobranca> buscarCobrancaPorId(@PathVariable Integer id) {
        Optional<FinanceiroCobranca> cobranca = financeiroCobrancaService.buscarCobrancaPorId(id);
        return cobranca.map(c -> new ResponseEntity<>(c, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<FinanceiroCobranca>> listarTodasCobrancas(
            @RequestParam(required = false) Integer condominioId,
            @RequestParam(required = false) Integer unidadeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVencimentoInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVencimentoFim) {
        
        List<FinanceiroCobranca> cobrancas = financeiroCobrancaService.listarTodasCobrancas(condominioId, unidadeId, status, dataVencimentoInicio, dataVencimentoFim);
        return new ResponseEntity<>(cobrancas, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or " +
                  "@financeiroCobrancaService.hasPermissionToManageCobrancaByCobrancaId(#id)")
    public ResponseEntity<FinanceiroCobranca> atualizarCobranca(@PathVariable Integer id, @RequestBody FinanceiroCobrancaRequestDTO requestDTO) {
        FinanceiroCobranca cobrancaSalva = financeiroCobrancaService.atualizarCobranca(id, requestDTO);
        return new ResponseEntity<>(cobrancaSalva, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or " +
                  "@financeiroCobrancaService.hasPermissionToManageCobrancaByCobrancaId(#id)")
    public ResponseEntity<Void> deletarCobranca(@PathVariable Integer id) {
        financeiroCobrancaService.deletarCobranca(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}