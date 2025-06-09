package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.Assembleia;
import br.com.gestaocondominio.api.domain.service.AssembleiaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/assembleias")
public class AssembleiaController {

    private final AssembleiaService assembleiaService;

    public AssembleiaController(AssembleiaService assembleiaService) {
        this.assembleiaService = assembleiaService;
    }

    @PostMapping
    public ResponseEntity<Assembleia> cadastrarAssembleia(@RequestBody Assembleia assembleia) {
        Assembleia novaAssembleia = assembleiaService.cadastrarAssembleia(assembleia);
        return new ResponseEntity<>(novaAssembleia, HttpStatus.CREATED); // 201 Created
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assembleia> buscarAssembleiaPorId(@PathVariable Integer id) {
        Optional<Assembleia> assembleia = assembleiaService.buscarAssembleiaPorId(id);
        return assembleia.map(a -> new ResponseEntity<>(a, HttpStatus.OK)) // 200 OK
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); // 404 Not Found
    }

    @GetMapping
    public ResponseEntity<List<Assembleia>> listarTodasAssembleias(@RequestParam(required = false, defaultValue = "true") boolean ativas) {
        // Por padrão, lista apenas ativas. Se o parâmetro 'ativas=false' for enviado, lista todas.
        List<Assembleia> assembleias = assembleiaService.listarTodasAssembleias(ativas);
        return new ResponseEntity<>(assembleias, HttpStatus.OK); // 200 OK
    }

    @PutMapping("/{id}")
    public ResponseEntity<Assembleia> atualizarAssembleia(@PathVariable Integer id, @RequestBody Assembleia assembleiaAtualizada) {
        Assembleia assembleiaSalva = assembleiaService.atualizarAssembleia(id, assembleiaAtualizada);
        return new ResponseEntity<>(assembleiaSalva, HttpStatus.OK); // 200 OK
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativarAssembleia(@PathVariable Integer id) {
        assembleiaService.inativarAssembleia(id); // Chama o método de inativação no serviço
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Assembleia> ativarAssembleia(@PathVariable Integer id) {
        Assembleia assembleiaAtivada = assembleiaService.ativarAssembleia(id);
        return new ResponseEntity<>(assembleiaAtivada, HttpStatus.OK); // 200 OK
    }
}