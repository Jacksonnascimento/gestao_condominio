package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.TaxaCondominioValor;
import br.com.gestaocondominio.api.domain.service.TaxaCondominioValorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/taxas/valores")
public class TaxaCondominioValorController {

    private final TaxaCondominioValorService taxaCondominioValorService;

    public TaxaCondominioValorController(TaxaCondominioValorService taxaCondominioValorService) {
        this.taxaCondominioValorService = taxaCondominioValorService;
    }

    @PostMapping
    public ResponseEntity<TaxaCondominioValor> criarOuAtualizarValor(@RequestBody TaxaCondominioValor taxaValor) {
        return new ResponseEntity<>(taxaCondominioValorService.salvar(taxaValor), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TaxaCondominioValor>> listarValores() {
        return ResponseEntity.ok(taxaCondominioValorService.listarTodos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarValor(@PathVariable Integer id) {
        taxaCondominioValorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}