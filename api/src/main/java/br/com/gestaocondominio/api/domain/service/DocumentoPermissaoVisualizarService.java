package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoVisualizar;
import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoId;
import br.com.gestaocondominio.api.domain.repository.DocumentoPermissaoVisualizarRepository;
import br.com.gestaocondominio.api.domain.repository.DocumentoRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentoPermissaoVisualizarService {

    private final DocumentoPermissaoVisualizarRepository documentoPermissaoVisualizarRepository;
    private final DocumentoRepository documentoRepository;
    private final PessoaRepository pessoaRepository;

    public DocumentoPermissaoVisualizarService(DocumentoPermissaoVisualizarRepository documentoPermissaoVisualizarRepository,
                                               DocumentoRepository documentoRepository,
                                               PessoaRepository pessoaRepository) {
        this.documentoPermissaoVisualizarRepository = documentoPermissaoVisualizarRepository;
        this.documentoRepository = documentoRepository;
        this.pessoaRepository = pessoaRepository;
    }

    public DocumentoPermissaoVisualizar cadastrarDocumentoPermissaoVisualizar(DocumentoPermissaoVisualizar permissao) {
        if (permissao.getDocumento() == null || permissao.getDocumento().getDocCod() == null) {
            throw new IllegalArgumentException("Documento deve ser informado para a permissão de visualização.");
        }
        documentoRepository.findById(permissao.getDocumento().getDocCod())
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado com o ID: " + permissao.getDocumento().getDocCod()));

        if (permissao.getPessoa() == null || permissao.getPessoa().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa deve ser informada para a permissão de visualização.");
        }
        pessoaRepository.findById(permissao.getPessoa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + permissao.getPessoa().getPesCod()));

        DocumentoPermissaoId id = new DocumentoPermissaoId(
            permissao.getDocumento().getDocCod(),
            permissao.getPessoa().getPesCod()
        );
        permissao.setId(id);

        if (documentoPermissaoVisualizarRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Esta permissão de visualização já existe para este documento e pessoa.");
        }

        return documentoPermissaoVisualizarRepository.save(permissao);
    }

    public Optional<DocumentoPermissaoVisualizar> buscarDocumentoPermissaoVisualizarPorId(DocumentoPermissaoId id) {
        return documentoPermissaoVisualizarRepository.findById(id);
    }

    public List<DocumentoPermissaoVisualizar> listarTodasDocumentoPermissoesVisualizar() {
        return documentoPermissaoVisualizarRepository.findAll();
    }

    public DocumentoPermissaoVisualizar atualizarDocumentoPermissaoVisualizar(DocumentoPermissaoId id, DocumentoPermissaoVisualizar permissaoAtualizada) {
        DocumentoPermissaoVisualizar permissaoExistente = documentoPermissaoVisualizarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permissão de visualização de documento não encontrada com o ID: " + id));

        if (!permissaoAtualizada.getDocumento().getDocCod().equals(permissaoExistente.getDocumento().getDocCod()) ||
            !permissaoAtualizada.getPessoa().getPesCod().equals(permissaoExistente.getPessoa().getPesCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Documento ou a Pessoa de uma permissão de visualização existente.");
        }
        

        return documentoPermissaoVisualizarRepository.save(permissaoExistente);
    }

    
    public void deletarPermissao(DocumentoPermissaoId id) {
        documentoPermissaoVisualizarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permissão de visualização de documento não encontrada para exclusão com o ID: " + id));

        documentoPermissaoVisualizarRepository.deleteById(id);
    }
}