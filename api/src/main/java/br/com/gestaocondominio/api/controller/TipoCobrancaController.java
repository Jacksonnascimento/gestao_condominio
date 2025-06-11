package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import br.com.gestaocondominio.api.domain.service.TipoCobrancaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cobrancas/tipos")
public class TipoCobrancaController {

    private final TipoCobrancaService tipoCobrancaService; 

    public TipoCobrancaController(TipoCobrancaService tipoCobrancaService) {
        this.tipoCobrancaService = tipoCobrancaService; 
    }

    @PostMapping
    public ResponseEntity<TipoCobranca> cadastrarTipoCobranca(@RequestBody TipoCobranca tipoCobranca) {
        TipoCobranca novoTipo = tipoCobrancaService.cadastrarTipoCobranca(tipoCobranca);
        return new ResponseEntity<>(novoTipo, HttpStatus.CREATED); 
    } 

    @GetMapping("/{id}")
    public ResponseEntity<TipoCobranca> buscarTipoCobrancaPorId(@PathVariable Integer id) {
        Optional<TipoCobranca> tipo = tipoCobrancaService.buscarTipoCobrancaPorId(id);
        return tipo.map(t -> new ResponseEntity<>(t, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<TipoCobranca>> listarTodosTiposCobranca(@RequestParam(required = false, defaultValue = "true") boolean ativos) {
        List<TipoCobranca> tipos = tipoCobrancaService.listarTodosTiposCobranca(ativos);
        return new ResponseEntity<>(tipos, HttpStatus.OK); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoCobranca> atualizarTipoCobranca(@PathVariable Integer id, @RequestBody TipoCobranca tipoCobrancaAtualizada) {
        TipoCobranca tipoSalvo = tipoCobrancaService.atualizarTipoCobranca(id, tipoCobrancaAtualizada);
        return new ResponseEntity<>(tipoSalvo, HttpStatus.OK); 
    }

    @PutMapping("/{id}/inativar") 
    public ResponseEntity<TipoCobranca> inativarTipoCobranca(@PathVariable Integer id) {
        TipoCobranca tipoInativado = tipoCobrancaService.inativarTipoCobranca(id);
        return new ResponseEntity<>(tipoInativado, HttpStatus.OK); 
    }

    @PutMapping("/{id}/ativar") 
    public ResponseEntity<TipoCobranca> ativarTipoCobranca(@PathVariable Integer id) {
        TipoCobranca tipoAtivado = tipoCobrancaService.ativarTipoCobranca(id);
        return new ResponseEntity<>(tipoAtivado, HttpStatus.OK); 
    }
}