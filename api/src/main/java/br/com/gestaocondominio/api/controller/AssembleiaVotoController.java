package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.AssembleiaVoto;
import br.com.gestaocondominio.api.domain.entity.AssembleiaVotoId;
import br.com.gestaocondominio.api.domain.service.AssembleiaVotoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/assembleias/votos")
public class AssembleiaVotoController {

    private final AssembleiaVotoService assembleiaVotoService;

    public AssembleiaVotoController(AssembleiaVotoService assembleiaVotoService) {
        this.assembleiaVotoService = assembleiaVotoService;
    }

    @PostMapping
    public ResponseEntity<AssembleiaVoto> cadastrarVoto(@RequestBody AssembleiaVoto voto) {
        AssembleiaVoto novoVoto = assembleiaVotoService.cadastrarAssembleiaVoto(voto);
        return new ResponseEntity<>(novoVoto, HttpStatus.CREATED);
    }

    @GetMapping("/{aspCod}/{pesCod}")
    public ResponseEntity<AssembleiaVoto> buscarVotoPorId(@PathVariable Integer aspCod, @PathVariable Integer pesCod) {
        AssembleiaVotoId id = new AssembleiaVotoId(aspCod, pesCod);
        Optional<AssembleiaVoto> voto = assembleiaVotoService.buscarVotoPorId(id);
        return voto.map(v -> new ResponseEntity<>(v, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/por-topico/{topicoId}")
    public ResponseEntity<Object> listarVotosPorTopico(@PathVariable Integer topicoId) {
        Object resultado = assembleiaVotoService.listarVotosPorTopico(topicoId);
        return new ResponseEntity<>(resultado, HttpStatus.OK);
    }

    @PutMapping("/{aspCod}/{pesCod}")
    public ResponseEntity<AssembleiaVoto> atualizarAssembleiaVoto(@PathVariable Integer aspCod, @PathVariable Integer pesCod, @RequestBody AssembleiaVoto votoAtualizado) {
        AssembleiaVotoId id = new AssembleiaVotoId(aspCod, pesCod);
        AssembleiaVoto votoSalvo = assembleiaVotoService.atualizarAssembleiaVoto(id, votoAtualizado);
        return new ResponseEntity<>(votoSalvo, HttpStatus.OK);
    }

    @DeleteMapping("/{aspCod}/{pesCod}")
    public ResponseEntity<Void> deletarVoto(@PathVariable Integer aspCod, @PathVariable Integer pesCod) {
        AssembleiaVotoId id = new AssembleiaVotoId(aspCod, pesCod);
        assembleiaVotoService.deletarVoto(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}