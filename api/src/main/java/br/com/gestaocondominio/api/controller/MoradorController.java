package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.Morador;
import br.com.gestaocondominio.api.domain.service.MoradorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/moradores")
public class MoradorController {

    private final MoradorService moradorService;

    public MoradorController(MoradorService moradorService) {
        this.moradorService = moradorService;
    }

    @PostMapping
    public ResponseEntity<Morador> cadastrarMorador(@RequestBody Morador morador) {
        Morador novoMorador = moradorService.cadastrarMorador(morador);
        return new ResponseEntity<>(novoMorador, HttpStatus.CREATED); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<Morador> buscarMoradorPorId(@PathVariable Integer id) {
        Optional<Morador> morador = moradorService.buscarMoradorPorId(id);
        return morador.map(m -> new ResponseEntity<>(m, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<Morador>> listarTodosMoradores() {
        List<Morador> moradores = moradorService.listarTodosMoradores();
        return new ResponseEntity<>(moradores, HttpStatus.OK); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<Morador> atualizarMorador(@PathVariable Integer id, @RequestBody Morador moradorAtualizado) {
        Morador moradorSalvo = moradorService.atualizarMorador(id, moradorAtualizado);
        return new ResponseEntity<>(moradorSalvo, HttpStatus.OK); 
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarMorador(@PathVariable Integer id) {
        
        moradorService.deletarMorador(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); 
    }
}