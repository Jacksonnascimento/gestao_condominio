package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.service.AreaComumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/areascomuns")
public class AreaComumController {

    private final AreaComumService areaComumService;

    public AreaComumController(AreaComumService areaComumService) {
        this.areaComumService = areaComumService;
    }

    @PostMapping
    public ResponseEntity<AreaComum> cadastrarAreaComum(@RequestBody AreaComum areaComum) {
        AreaComum novaAreaComum = areaComumService.cadastrarAreaComum(areaComum);
        return new ResponseEntity<>(novaAreaComum, HttpStatus.CREATED); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<AreaComum> buscarAreaComumPorId(@PathVariable Integer id) {
        Optional<AreaComum> areaComum = areaComumService.buscarAreaComumPorId(id);
        return areaComum.map(ac -> new ResponseEntity<>(ac, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<AreaComum>> listarTodasAreasComuns() {
        List<AreaComum> areasComuns = areaComumService.listarTodasAreasComuns();
        return new ResponseEntity<>(areasComuns, HttpStatus.OK); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<AreaComum> atualizarAreaComum(@PathVariable Integer id, @RequestBody AreaComum areaComumAtualizada) {
        AreaComum areaComumSalva = areaComumService.atualizarAreaComum(id, areaComumAtualizada);
        return new ResponseEntity<>(areaComumSalva, HttpStatus.OK); 
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAreaComum(@PathVariable Integer id) {
        areaComumService.deletarAreaComum(id); 
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); 
    }
}