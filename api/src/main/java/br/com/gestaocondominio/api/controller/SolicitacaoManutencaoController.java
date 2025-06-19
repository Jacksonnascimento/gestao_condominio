package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.SolicitacaoManutencao;
import br.com.gestaocondominio.api.domain.service.SolicitacaoManutencaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/manutencoes")
public class SolicitacaoManutencaoController {

    private final SolicitacaoManutencaoService solicitacaoManutencaoService;

    public SolicitacaoManutencaoController(SolicitacaoManutencaoService solicitacaoManutencaoService) {
        this.solicitacaoManutencaoService = solicitacaoManutencaoService;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoManutencao> cadastrarSolicitacaoManutencao(@RequestBody SolicitacaoManutencao solicitacao) {
        SolicitacaoManutencao novaSolicitacao = solicitacaoManutencaoService.cadastrarSolicitacaoManutencao(solicitacao);
        return new ResponseEntity<>(novaSolicitacao, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitacaoManutencao> buscarSolicitacaoManutencaoPorId(@PathVariable Integer id) {
        Optional<SolicitacaoManutencao> solicitacao = solicitacaoManutencaoService.buscarSolicitacaoManutencaoPorId(id);
        return solicitacao.map(s -> new ResponseEntity<>(s, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<SolicitacaoManutencao>> listarTodasSolicitacoesManutencao() {
        List<SolicitacaoManutencao> solicitacoes = solicitacaoManutencaoService.listarTodasSolicitacoesManutencao();
        return new ResponseEntity<>(solicitacoes, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SolicitacaoManutencao> atualizarSolicitacaoManutencao(@PathVariable Integer id, @RequestBody SolicitacaoManutencao solicitacaoAtualizada) {
        SolicitacaoManutencao solicitacaoSalva = solicitacaoManutencaoService.atualizarSolicitacaoManutencao(id, solicitacaoAtualizada);
        return new ResponseEntity<>(solicitacaoSalva, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarSolicitacaoManutencao(@PathVariable Integer id) {
        solicitacaoManutencaoService.deletarSolicitacaoManutencao(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}