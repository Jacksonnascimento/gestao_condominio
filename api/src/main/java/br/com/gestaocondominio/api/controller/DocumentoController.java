package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.Documento;
import br.com.gestaocondominio.api.domain.service.DocumentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;

    public DocumentoController(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @PostMapping
    public ResponseEntity<Documento> cadastrarDocumento(@RequestBody Documento documento) {
        Documento novoDocumento = documentoService.cadastrarDocumento(documento);
        return new ResponseEntity<>(novoDocumento, HttpStatus.CREATED); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<Documento> buscarDocumentoPorId(@PathVariable Integer id) {
        Optional<Documento> documento = documentoService.buscarDocumentoPorId(id);
        return documento.map(d -> new ResponseEntity<>(d, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<Documento>> listarTodosDocumentos() {
        List<Documento> documentos = documentoService.listarTodosDocumentos();
        return new ResponseEntity<>(documentos, HttpStatus.OK); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<Documento> atualizarDocumento(@PathVariable Integer id, @RequestBody Documento documentoAtualizado) {
        Documento documentoSalvo = documentoService.atualizarDocumento(id, documentoAtualizado);
        return new ResponseEntity<>(documentoSalvo, HttpStatus.OK); 
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarDocumento(@PathVariable Integer id) {
        documentoService.deletarDocumento(id); 
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); 
    }
}