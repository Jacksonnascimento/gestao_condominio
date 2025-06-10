package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Documento;
import br.com.gestaocondominio.api.domain.repository.DocumentoRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.DocumentoPermissaoVisualizarRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final CondominioRepository condominioRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final PessoaRepository pessoaRepository;
    private final DocumentoPermissaoVisualizarRepository documentoPermissaoVisualizarRepository;

    public DocumentoService(DocumentoRepository documentoRepository,
                            CondominioRepository condominioRepository,
                            AssembleiaRepository assembleiaRepository,
                            PessoaRepository pessoaRepository,
                            DocumentoPermissaoVisualizarRepository documentoPermissaoVisualizarRepository) {
        this.documentoRepository = documentoRepository;
        this.condominioRepository = condominioRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.pessoaRepository = pessoaRepository;
        this.documentoPermissaoVisualizarRepository = documentoPermissaoVisualizarRepository;
    }

    public Documento cadastrarDocumento(Documento documento) {
        if (documento.getCondominio() == null || documento.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para o documento.");
        }
        condominioRepository.findById(documento.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + documento.getCondominio().getConCod()));

        if (documento.getAssembleia() != null && documento.getAssembleia().getAssCod() != null) {
            assembleiaRepository.findById(documento.getAssembleia().getAssCod())
                    .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + documento.getAssembleia().getAssCod()));
        }

        if (documento.getUploader() == null || documento.getUploader().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa responsável pelo upload deve ser informada para o documento.");
        }
        pessoaRepository.findById(documento.getUploader().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa responsável pelo upload não encontrada com o ID: " + documento.getUploader().getPesCod()));

        if (documento.getPermissaoVisualizar() == null) {
            documento.setPermissaoVisualizar('T');
        }

        documento.setDtUpload(LocalDateTime.now());
        documento.setDtAtualizacao(LocalDateTime.now());

        return documentoRepository.save(documento);
    }

    public Optional<Documento> buscarDocumentoPorId(Integer id) {
        return documentoRepository.findById(id);
    }

    public List<Documento> listarTodosDocumentos() {
        return documentoRepository.findAll();
    }

    public Documento atualizarDocumento(Integer id, Documento documentoAtualizado) { 
        Documento documentoExistente = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado com o ID: " + id));

        if (documentoAtualizado.getCondominio() != null && !documentoAtualizado.getCondominio().getConCod().equals(documentoExistente.getCondominio().getConCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Condomínio de um documento existente.");
        }
        if (documentoAtualizado.getAssembleia() != null &&
            (documentoExistente.getAssembleia() == null || !documentoAtualizado.getAssembleia().getAssCod().equals(documentoExistente.getAssembleia().getAssCod()))) {
            assembleiaRepository.findById(documentoAtualizado.getAssembleia().getAssCod()) // <-- Uso da variável corrigida
                    .orElseThrow(() -> new IllegalArgumentException("Nova Assembleia para o documento não encontrada com o ID: " + documentoAtualizado.getAssembleia().getAssCod()));
            documentoExistente.setAssembleia(documentoAtualizado.getAssembleia());
        } else if (documentoAtualizado.getAssembleia() == null && documentoExistente.getAssembleia() != null) {
            documentoExistente.setAssembleia(null);
        }

        if (documentoAtualizado.getUploader() != null && !documentoAtualizado.getUploader().getPesCod().equals(documentoExistente.getUploader().getPesCod())) {
            throw new IllegalArgumentException("Não é permitido alterar a Pessoa Uploader de um documento existente.");
        }

        if (documentoAtualizado.getNome() != null) {
            documentoExistente.setNome(documentoAtualizado.getNome());
        }
        if (documentoAtualizado.getTipo() != null) {
            documentoExistente.setTipo(documentoAtualizado.getTipo());
        }
        if (documentoAtualizado.getCaminhoArquivo() != null) {
            documentoExistente.setCaminhoArquivo(documentoAtualizado.getCaminhoArquivo());
        }
        if (documentoAtualizado.getPermissaoVisualizar() != null) {
            documentoExistente.setPermissaoVisualizar(documentoAtualizado.getPermissaoVisualizar());
        }

        documentoExistente.setDtAtualizacao(LocalDateTime.now());
        return documentoRepository.save(documentoExistente);
    }

    @Transactional
    public void deletarDocumento(Integer id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado com o ID: " + id));

        List<br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoVisualizar> permissoes = documentoPermissaoVisualizarRepository.findByDocumento(documento);
        if (!permissoes.isEmpty()) {
            documentoPermissaoVisualizarRepository.deleteAll(permissoes);
        }

        documentoRepository.deleteById(id);
    }
}