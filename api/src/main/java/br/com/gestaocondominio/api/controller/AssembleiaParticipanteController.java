package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipante;
import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipanteId;
import br.com.gestaocondominio.api.domain.service.AssembleiaParticipanteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/assembleias/participantes")
public class AssembleiaParticipanteController {

    private final AssembleiaParticipanteService assembleiaParticipanteService;

    public AssembleiaParticipanteController(AssembleiaParticipanteService assembleiaParticipanteService) {
        this.assembleiaParticipanteService = assembleiaParticipanteService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or @assembleiaParticipanteService.temPermissaoParaGerenciarParticipantes(#participante.assembleia.assCod)")
    public ResponseEntity<AssembleiaParticipante> cadastrarParticipante(@RequestBody AssembleiaParticipante participante) {
        AssembleiaParticipante novoParticipante = assembleiaParticipanteService.cadastrarAssembleiaParticipante(participante);
        return new ResponseEntity<>(novoParticipante, HttpStatus.CREATED);
    }

    @GetMapping("/{assCod}/{pesCod}")
    public ResponseEntity<AssembleiaParticipante> buscarParticipantePorId(@PathVariable Integer assCod, @PathVariable Integer pesCod) {
        AssembleiaParticipanteId id = new AssembleiaParticipanteId(assCod, pesCod);
        Optional<AssembleiaParticipante> participante = assembleiaParticipanteService.buscarAssembleiaParticipantePorId(id);
        return participante.map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/por-assembleia/{assembleiaId}")
    public ResponseEntity<List<AssembleiaParticipante>> listarParticipantesPorAssembleia(@PathVariable Integer assembleiaId) {
        List<AssembleiaParticipante> participantes = assembleiaParticipanteService.listarParticipantesPorAssembleia(assembleiaId);
        return new ResponseEntity<>(participantes, HttpStatus.OK);
    }

    @PutMapping("/{assCod}/{pesCod}")
    public ResponseEntity<AssembleiaParticipante> atualizarParticipante(@PathVariable Integer assCod, @PathVariable Integer pesCod, @RequestBody AssembleiaParticipante participanteAtualizado) {
        AssembleiaParticipanteId id = new AssembleiaParticipanteId(assCod, pesCod);
        AssembleiaParticipante participanteSalvo = assembleiaParticipanteService.atualizarAssembleiaParticipante(id, participanteAtualizado);
        return new ResponseEntity<>(participanteSalvo, HttpStatus.OK);
    }

    @DeleteMapping("/{assCod}/{pesCod}")
    public ResponseEntity<Void> deletarParticipante(@PathVariable Integer assCod, @PathVariable Integer pesCod) {
        AssembleiaParticipanteId id = new AssembleiaParticipanteId(assCod, pesCod);
        assembleiaParticipanteService.deletarParticipante(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}