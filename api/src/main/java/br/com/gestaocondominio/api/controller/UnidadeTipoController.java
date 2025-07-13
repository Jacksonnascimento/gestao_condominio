package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.UnidadeTipo;
import br.com.gestaocondominio.api.domain.service.UnidadeTipoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades/tipos")
public class UnidadeTipoController {

    private final UnidadeTipoService unidadeTipoService;

    public UnidadeTipoController(UnidadeTipoService unidadeTipoService) {
        this.unidadeTipoService = unidadeTipoService;
    }

    @PostMapping
    public ResponseEntity<UnidadeTipo> criarTipoUnidade(@RequestBody UnidadeTipo unidadeTipo) {
        return new ResponseEntity<>(unidadeTipoService.criar(unidadeTipo), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UnidadeTipo>> listarTiposUnidade() {
        return ResponseEntity.ok(unidadeTipoService.listarTodos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarTipoUnidade(@PathVariable Integer id) {
        unidadeTipoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}