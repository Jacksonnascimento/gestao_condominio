package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.ComunicadoEntrega;
import br.com.gestaocondominio.api.domain.service.ComunicadoEntregaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/comunicados/entregas") 
public class ComunicadoEntregaController {

    private final ComunicadoEntregaService comunicadoEntregaService;

    public ComunicadoEntregaController(ComunicadoEntregaService comunicadoEntregaService) {
        this.comunicadoEntregaService = comunicadoEntregaService;
    }

    @PostMapping
    public ResponseEntity<ComunicadoEntrega> cadastrarComunicadoEntrega(@RequestBody ComunicadoEntrega comunicadoEntrega) {
        ComunicadoEntrega novaEntrega = comunicadoEntregaService.cadastrarComunicadoEntrega(comunicadoEntrega);
        return new ResponseEntity<>(novaEntrega, HttpStatus.CREATED); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComunicadoEntrega> buscarComunicadoEntregaPorId(@PathVariable Integer id) {
        Optional<ComunicadoEntrega> comunicadoEntrega = comunicadoEntregaService.buscarComunicadoEntregaPorId(id);
        return comunicadoEntrega.map(ce -> new ResponseEntity<>(ce, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<ComunicadoEntrega>> listarTodasComunicadoEntregas() {
        List<ComunicadoEntrega> entregas = comunicadoEntregaService.listarTodasComunicadoEntregas();
        return new ResponseEntity<>(entregas, HttpStatus.OK); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComunicadoEntrega> atualizarComunicadoEntrega(@PathVariable Integer id, @RequestBody ComunicadoEntrega comunicadoEntregaAtualizada) {
        ComunicadoEntrega entregaSalva = comunicadoEntregaService.atualizarComunicadoEntrega(id, comunicadoEntregaAtualizada);
        return new ResponseEntity<>(entregaSalva, HttpStatus.OK); 
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarComunicadoEntrega(@PathVariable Integer id) {
       
        comunicadoEntregaService.deletarComunicadoEntrega(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); 
    }
}