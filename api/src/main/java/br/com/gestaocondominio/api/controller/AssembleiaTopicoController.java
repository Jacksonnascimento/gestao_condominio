package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.AssembleiaTopico;
import br.com.gestaocondominio.api.domain.service.AssembleiaTopicoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/assembleias/topicos")
public class AssembleiaTopicoController {

    private final AssembleiaTopicoService assembleiaTopicoService;

    public AssembleiaTopicoController(AssembleiaTopicoService assembleiaTopicoService) {
        this.assembleiaTopicoService = assembleiaTopicoService;
    }

    @PostMapping
    public ResponseEntity<AssembleiaTopico> cadastrarTopico(@RequestBody AssembleiaTopico topico) {
        AssembleiaTopico novoTopico = assembleiaTopicoService.cadastrarAssembleiaTopico(topico);
        return new ResponseEntity<>(novoTopico, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssembleiaTopico> buscarTopicoPorId(@PathVariable Integer id) {
        Optional<AssembleiaTopico> topico = assembleiaTopicoService.buscarAssembleiaTopicoPorId(id);
        return topico.map(t -> new ResponseEntity<>(t, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<AssembleiaTopico>> listarTodosTopicos() {
        List<AssembleiaTopico> topicos = assembleiaTopicoService.listarTodosTopicos();
        return new ResponseEntity<>(topicos, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssembleiaTopico> atualizarTopico(@PathVariable Integer id, @RequestBody AssembleiaTopico topicoAtualizado) {
        AssembleiaTopico topicoSalvo = assembleiaTopicoService.atualizarTopico(id, topicoAtualizado);
        return new ResponseEntity<>(topicoSalvo, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarTopico(@PathVariable Integer id) {
        assembleiaTopicoService.deletarTopico(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}