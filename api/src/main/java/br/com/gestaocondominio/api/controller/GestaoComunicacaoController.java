package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.GestaoComunicacao;
import br.com.gestaocondominio.api.domain.service.GestaoComunicacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/comunicacoes")
public class GestaoComunicacaoController {

    private final GestaoComunicacaoService gestaoComunicacaoService;

    public GestaoComunicacaoController(GestaoComunicacaoService gestaoComunicacaoService) {
        this.gestaoComunicacaoService = gestaoComunicacaoService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or hasAnyAuthority('ROLE_SINDICO_' + #comunicacao.condominio.conCod, 'ROLE_ADMIN_' + #comunicacao.condominio.conCod, 'ROLE_MORADOR_' + #comunicacao.condominio.conCod)")
    public ResponseEntity<GestaoComunicacao> cadastrarComunicacao(@RequestBody GestaoComunicacao comunicacao) {
        GestaoComunicacao novaComunicacao = gestaoComunicacaoService.cadastrarComunicacao(comunicacao);
        return new ResponseEntity<>(novaComunicacao, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<GestaoComunicacao> buscarComunicacaoPorId(@PathVariable Integer id) {
        Optional<GestaoComunicacao> comunicacao = gestaoComunicacaoService.buscarComunicacaoPorId(id);
        return comunicacao.map(c -> new ResponseEntity<>(c, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<GestaoComunicacao>> listarTodasComunicacoes() {
        List<GestaoComunicacao> comunicacoes = gestaoComunicacaoService.listarTodasComunicacoes();
        return new ResponseEntity<>(comunicacoes, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GestaoComunicacao> atualizarComunicacao(@PathVariable Integer id, @RequestBody GestaoComunicacao comunicacaoAtualizada) {
        GestaoComunicacao comunicacaoSalva = gestaoComunicacaoService.atualizarComunicacao(id, comunicacaoAtualizada);
        return new ResponseEntity<>(comunicacaoSalva, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarComunicacao(@PathVariable Integer id) {
        gestaoComunicacaoService.deletarComunicacao(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}