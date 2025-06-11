package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.service.CondominioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/condominios")
public class CondominioController {

    private final CondominioService condominioService;

    public CondominioController(CondominioService condominioService) {
        this.condominioService = condominioService;
    }

    @PostMapping
    public ResponseEntity<Condominio> cadastrarCondominio(@RequestBody Condominio condominio) {
        Condominio novoCondominio = condominioService.cadastrarCondominio(condominio);
        return new ResponseEntity<>(novoCondominio, HttpStatus.CREATED); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<Condominio> buscarCondominioPorId(@PathVariable Integer id) {
        Optional<Condominio> condominio = condominioService.buscarCondominioPorId(id);
        return condominio.map(c -> new ResponseEntity<>(c, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<Condominio>> listarTodosCondominios(@RequestParam(required = false, defaultValue = "true") boolean ativas) {
        List<Condominio> condominios = condominioService.listarTodosCondominios(ativas);
        return new ResponseEntity<>(condominios, HttpStatus.OK); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<Condominio> atualizarCondominio(@PathVariable Integer id, @RequestBody Condominio condominioAtualizado) { 
        Condominio condominioSalva = condominioService.atualizarCondominio(id, condominioAtualizado); 
        return new ResponseEntity<>(condominioSalva, HttpStatus.OK); 
    }

    @PutMapping("/{id}/inativar") 
    public ResponseEntity<Condominio> inativarCondominio(@PathVariable Integer id) {
        Condominio condominioInativado = condominioService.inativarCondominio(id);
        return new ResponseEntity<>(condominioInativado, HttpStatus.OK); 
    }

    @PutMapping("/{id}/ativar") 
    public ResponseEntity<Condominio> ativarCondominio(@PathVariable Integer id) {
        Condominio condominioAtivado = condominioService.ativarCondominio(id);
        return new ResponseEntity<>(condominioAtivado, HttpStatus.OK); 
    }
}