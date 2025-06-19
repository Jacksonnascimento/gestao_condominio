package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoId;
import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoVisualizar;
import br.com.gestaocondominio.api.domain.service.DocumentoPermissaoVisualizarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/documentos/permissoes")
public class DocumentoPermissaoVisualizarController {

    private final DocumentoPermissaoVisualizarService documentoPermissaoVisualizarService;

    public DocumentoPermissaoVisualizarController(DocumentoPermissaoVisualizarService documentoPermissaoVisualizarService) {
        this.documentoPermissaoVisualizarService = documentoPermissaoVisualizarService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or @documentoPermissaoVisualizarService.temPermissaoParaGerenciar(#permissao.documento.docCod)")
    public ResponseEntity<DocumentoPermissaoVisualizar> cadastrarPermissao(@RequestBody DocumentoPermissaoVisualizar permissao) {
        DocumentoPermissaoVisualizar novaPermissao = documentoPermissaoVisualizarService.cadastrarDocumentoPermissaoVisualizar(permissao);
        return new ResponseEntity<>(novaPermissao, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DocumentoPermissaoVisualizar>> listarTodasPermissoes() {
        List<DocumentoPermissaoVisualizar> permissoes = documentoPermissaoVisualizarService.listarTodasDocumentoPermissoesVisualizar();
        return new ResponseEntity<>(permissoes, HttpStatus.OK);
    }

    @DeleteMapping("/{docCod}/{pesCod}")
    public ResponseEntity<Void> deletarPermissao(@PathVariable Integer docCod, @PathVariable Integer pesCod) {
        documentoPermissaoVisualizarService.deletarPermissao(new DocumentoPermissaoId(docCod, pesCod));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}