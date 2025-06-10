package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.Administradora;
import br.com.gestaocondominio.api.domain.service.AdministradoraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/administradoras")
public class AdministradoraController {

    private final AdministradoraService administradoraService;

    public AdministradoraController(AdministradoraService administradoraService) {
        this.administradoraService = administradoraService;
    }

    @PostMapping
    public ResponseEntity<Administradora> cadastrarAdministradora(@RequestBody Administradora administradora) {
        Administradora novaAdministradora = administradoraService.cadastrarAdministradora(administradora);
        return new ResponseEntity<>(novaAdministradora, HttpStatus.CREATED); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<Administradora> buscarAdministradoraPorId(@PathVariable Integer id) {
        Optional<Administradora> administradora = administradoraService.buscarAdministradoraPorId(id);
        return administradora.map(a -> new ResponseEntity<>(a, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<Administradora>> listarTodasAdministradoras(@RequestParam(required = false, defaultValue = "true") boolean ativas) {
        
        List<Administradora> administradoras = administradoraService.listarTodasAdministradoras(ativas);
        return new ResponseEntity<>(administradoras, HttpStatus.OK); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<Administradora> atualizarAdministradora(@PathVariable Integer id, @RequestBody Administradora administradoraAtualizada) {
        Administradora administradoraSalva = administradoraService.atualizarAdministradora(id, administradoraAtualizada);
        return new ResponseEntity<>(administradoraSalva, HttpStatus.OK); 
    }

  
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativarAdministradora(@PathVariable Integer id) {
        administradoraService.inativarAdministradora(id); 
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); 
    }

    
    @PutMapping("/{id}/ativar")
    public ResponseEntity<Administradora> ativarAdministradora(@PathVariable Integer id) {
        Administradora administradoraAtivada = administradoraService.ativarAdministradora(id);
        return new ResponseEntity<>(administradoraAtivada, HttpStatus.OK); 
    }
}