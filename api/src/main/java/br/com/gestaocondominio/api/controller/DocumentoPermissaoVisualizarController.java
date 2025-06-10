package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoVisualizar;
import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoId; 
import br.com.gestaocondominio.api.domain.service.DocumentoPermissaoVisualizarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documentos/permissoes") 
public class DocumentoPermissaoVisualizarController {

    private final DocumentoPermissaoVisualizarService documentoPermissaoVisualizarService;

    public DocumentoPermissaoVisualizarController(DocumentoPermissaoVisualizarService documentoPermissaoVisualizarService) {
        this.documentoPermissaoVisualizarService = documentoPermissaoVisualizarService;
    }

    @PostMapping
    public ResponseEntity<DocumentoPermissaoVisualizar> cadastrarPermissao(@RequestBody DocumentoPermissaoVisualizar permissao) {
        DocumentoPermissaoVisualizar novaPermissao = documentoPermissaoVisualizarService.cadastrarDocumentoPermissaoVisualizar(permissao);
        return new ResponseEntity<>(novaPermissao, HttpStatus.CREATED); 
    }

    
    @GetMapping("/{docCod}/{pesCod}")
    public ResponseEntity<DocumentoPermissaoVisualizar> buscarPermissaoPorId(@PathVariable Integer docCod, @PathVariable Integer pesCod) {
        DocumentoPermissaoId id = new DocumentoPermissaoId(docCod, pesCod);
        Optional<DocumentoPermissaoVisualizar> permissao = documentoPermissaoVisualizarService.buscarDocumentoPermissaoVisualizarPorId(id);
        return permissao.map(p -> new ResponseEntity<>(p, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<DocumentoPermissaoVisualizar>> listarTodasPermissoes() {
        List<DocumentoPermissaoVisualizar> permissoes = documentoPermissaoVisualizarService.listarTodasDocumentoPermissoesVisualizar();
        return new ResponseEntity<>(permissoes, HttpStatus.OK); 
    }

  
    @PutMapping("/{docCod}/{pesCod}")
    public ResponseEntity<DocumentoPermissaoVisualizar> atualizarPermissao(@PathVariable Integer docCod, @PathVariable Integer pesCod, @RequestBody DocumentoPermissaoVisualizar permissaoAtualizada) {
        DocumentoPermissaoId id = new DocumentoPermissaoId(docCod, pesCod);
        
        DocumentoPermissaoVisualizar permissaoSalva = documentoPermissaoVisualizarService.atualizarDocumentoPermissaoVisualizar(id, permissaoAtualizada);
        return new ResponseEntity<>(permissaoSalva, HttpStatus.OK); 
    }

  
    @DeleteMapping("/{docCod}/{pesCod}")
    public ResponseEntity<Void> deletarPermissao(@PathVariable Integer docCod, @PathVariable Integer pesCod) {
        DocumentoPermissaoId id = new DocumentoPermissaoId(docCod, pesCod);
        documentoPermissaoVisualizarService.deletarPermissao(id); 
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); 
    }
}