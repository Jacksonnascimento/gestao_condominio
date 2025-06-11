package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.TipoSolicitacaoManutencao;
import br.com.gestaocondominio.api.domain.service.TipoSolicitacaoManutencaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/manutencoes/tipos")
public class TipoSolicitacaoManutencaoController {

    private final TipoSolicitacaoManutencaoService tipoSolicitacaoManutencaoService;

    public TipoSolicitacaoManutencaoController(TipoSolicitacaoManutencaoService tipoSolicitacaoManutencaoService) {
        this.tipoSolicitacaoManutencaoService = tipoSolicitacaoManutencaoService;
    }

    @PostMapping
    public ResponseEntity<TipoSolicitacaoManutencao> cadastrarTipoSolicitacaoManutencao(
            @RequestBody TipoSolicitacaoManutencao tipoSolicitacao) {
        TipoSolicitacaoManutencao novoTipo = tipoSolicitacaoManutencaoService
                .cadastrarTipoSolicitacaoManutencao(tipoSolicitacao);
        return new ResponseEntity<>(novoTipo, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoSolicitacaoManutencao> buscarTipoSolicitacaoManutencaoPorId(@PathVariable Integer id) {
        Optional<TipoSolicitacaoManutencao> tipo = tipoSolicitacaoManutencaoService
                .buscarTipoSolicitacaoManutencaoPorId(id);
        return tipo.map(t -> new ResponseEntity<>(t, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<TipoSolicitacaoManutencao>> listarTodosTiposSolicitacaoManutencao(
            @RequestParam(required = false, defaultValue = "true") boolean ativos) {
        List<TipoSolicitacaoManutencao> tipos = tipoSolicitacaoManutencaoService
                .listarTodosTiposSolicitacaoManutencao(ativos);
        return new ResponseEntity<>(tipos, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoSolicitacaoManutencao> atualizarTipoSolicitacaoManutencao(@PathVariable Integer id,
            @RequestBody TipoSolicitacaoManutencao tipoSolicitacaoAtualizada) {
        TipoSolicitacaoManutencao tipoSalvo = tipoSolicitacaoManutencaoService.atualizarTipoSolicitacaoManutencao(id,
                tipoSolicitacaoAtualizada);
        return new ResponseEntity<>(tipoSalvo, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativarTipoSolicitacaoManutencao(@PathVariable Integer id) {
        tipoSolicitacaoManutencaoService.inativarTipoSolicitacaoManutencao(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<TipoSolicitacaoManutencao> ativarTipoSolicitacaoManutencao(@PathVariable Integer id) {
        TipoSolicitacaoManutencao tipoAtivado = tipoSolicitacaoManutencaoService.ativarTipoSolicitacaoManutencao(id);
        return new ResponseEntity<>(tipoAtivado, HttpStatus.OK);
    }
}