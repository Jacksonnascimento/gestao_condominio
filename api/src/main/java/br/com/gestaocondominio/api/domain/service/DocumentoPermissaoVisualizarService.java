package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Documento;
import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoId;
import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoVisualizar;
import br.com.gestaocondominio.api.domain.repository.DocumentoPermissaoVisualizarRepository;
import br.com.gestaocondominio.api.domain.repository.DocumentoRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.security.access.AccessDeniedException; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("documentoPermissaoVisualizarService")
public class DocumentoPermissaoVisualizarService {

    private final DocumentoPermissaoVisualizarRepository documentoPermissaoVisualizarRepository;
    private final DocumentoRepository documentoRepository;
    private final PessoaRepository pessoaRepository;
    private final DocumentoService documentoService;

    public DocumentoPermissaoVisualizarService(DocumentoPermissaoVisualizarRepository documentoPermissaoVisualizarRepository, DocumentoRepository documentoRepository, PessoaRepository pessoaRepository, DocumentoService documentoService) {
        this.documentoPermissaoVisualizarRepository = documentoPermissaoVisualizarRepository;
        this.documentoRepository = documentoRepository;
        this.pessoaRepository = pessoaRepository;
        this.documentoService = documentoService;
    }

    public DocumentoPermissaoVisualizar cadastrarDocumentoPermissaoVisualizar(DocumentoPermissaoVisualizar permissao) {
        documentoRepository.findById(permissao.getDocumento().getDocCod()).orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
        pessoaRepository.findById(permissao.getPessoa().getPesCod()).orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada"));

        permissao.setId(new DocumentoPermissaoId(permissao.getDocumento().getDocCod(), permissao.getPessoa().getPesCod()));

        if (documentoPermissaoVisualizarRepository.existsById(permissao.getId())) {
            throw new IllegalArgumentException("Esta permissão de visualização já existe.");
        }
        return documentoPermissaoVisualizarRepository.save(permissao);
    }
    
    public List<DocumentoPermissaoVisualizar> listarTodasDocumentoPermissoesVisualizar() {
        return documentoPermissaoVisualizarRepository.findAll();
    }

    @Transactional
    public void deletarPermissao(DocumentoPermissaoId id) {
        DocumentoPermissaoVisualizar permissao = documentoPermissaoVisualizarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permissão não encontrada."));
        
        if (!documentoService.temPermissaoParaGerenciar(permissao.getDocumento().getCondominio().getConCod())) {
            // --- LINHA CORRIGIDA ---
            throw new AccessDeniedException("Acesso negado.");
        }
        
        documentoPermissaoVisualizarRepository.delete(permissao);
    }

    public boolean temPermissaoParaGerenciar(Integer docCod) {
        Documento doc = documentoRepository.findById(docCod)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado com o ID: " + docCod));
        return documentoService.temPermissaoParaGerenciar(doc.getCondominio().getConCod());
    }
}