package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @PostMapping
    public ResponseEntity<Unidade> cadastrarUnidade(@RequestBody Unidade unidade) {
        Unidade novaUnidade = unidadeService.cadastrarUnidade(unidade);
        return new ResponseEntity<>(novaUnidade, HttpStatus.CREATED); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<Unidade> buscarUnidadePorId(@PathVariable Integer id) {
        Optional<Unidade> unidade = unidadeService.buscarUnidadePorId(id);
        return unidade.map(u -> new ResponseEntity<>(u, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<Unidade>> listarTodasUnidades(@RequestParam(required = false, defaultValue = "true") boolean ativas) {
        List<Unidade> unidades = unidadeService.listarTodasUnidades(ativas);
        return new ResponseEntity<>(unidades, HttpStatus.OK); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<Unidade> atualizarUnidade(@PathVariable Integer id, @RequestBody Unidade unidadeAtualizada) {
        Unidade unidadeSalva = unidadeService.atualizarUnidade(id, unidadeAtualizada);
        return new ResponseEntity<>(unidadeSalva, HttpStatus.OK); 
    }

    @PutMapping("/{id}/inativar")
    public ResponseEntity<Unidade> inativarUnidade(@PathVariable Integer id) {
        Unidade unidadeInativada = unidadeService.inativarUnidade(id);
        return new ResponseEntity<>(unidadeInativada, HttpStatus.OK); 
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Unidade> ativarUnidade(@PathVariable Integer id) {
        Unidade unidadeAtivada = unidadeService.ativarUnidade(id);
        return new ResponseEntity<>(unidadeAtivada, HttpStatus.OK); 
    }
}