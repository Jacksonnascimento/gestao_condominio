package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.Assembleia;
import br.com.gestaocondominio.api.domain.service.AssembleiaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or hasAnyAuthority('ROLE_SINDICO_' + #assembleia.condominio.conCod, 'ROLE_ADMIN_' + #assembleia.condominio.conCod)")
    public ResponseEntity<Assembleia> cadastrarAssembleia(@RequestBody Assembleia assembleia) {
        Assembleia novaAssembleia = assembleiaService.cadastrarAssembleia(assembleia);
        return new ResponseEntity<>(novaAssembleia, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assembleia> buscarAssembleiaPorId(@PathVariable Integer id) {
        Optional<Assembleia> assembleia = assembleiaService.buscarAssembleiaPorId(id);
        return assembleia.map(a -> new ResponseEntity<>(a, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<Assembleia>> listarTodasAssembleias(@RequestParam(name = "ativas", defaultValue = "true") boolean ativas) {
        List<Assembleia> assembleias = assembleiaService.listarTodasAssembleias(ativas);
        return new ResponseEntity<>(assembleias, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Assembleia> atualizarAssembleia(@PathVariable Integer id, @RequestBody Assembleia assembleiaAtualizada) {
        Assembleia assembleiaSalva = assembleiaService.atualizarAssembleia(id, assembleiaAtualizada);
        return new ResponseEntity<>(assembleiaSalva, HttpStatus.OK);
    }

    @PutMapping("/{id}/inativar")
    public ResponseEntity<Assembleia> inativarAssembleia(@PathVariable Integer id) {
        Assembleia assembleiaInativada = assembleiaService.inativarAssembleia(id);
        return new ResponseEntity<>(assembleiaInativada, HttpStatus.OK);
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Assembleia> ativarAssembleia(@PathVariable Integer id) {
        Assembleia assembleiaAtivada = assembleiaService.ativarAssembleia(id);
        return new ResponseEntity<>(assembleiaAtivada, HttpStatus.OK);
    }
}