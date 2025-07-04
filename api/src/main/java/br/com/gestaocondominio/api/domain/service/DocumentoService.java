package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Documento;
import br.com.gestaocondominio.api.domain.repository.AssembleiaRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.DocumentoPermissaoVisualizarRepository;
import br.com.gestaocondominio.api.domain.repository.DocumentoRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("documentoService")
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final CondominioRepository condominioRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final PessoaRepository pessoaRepository;
    private final DocumentoPermissaoVisualizarRepository documentoPermissaoVisualizarRepository;

    public DocumentoService(DocumentoRepository documentoRepository, CondominioRepository condominioRepository, AssembleiaRepository assembleiaRepository, PessoaRepository pessoaRepository, DocumentoPermissaoVisualizarRepository documentoPermissaoVisualizarRepository) {
        this.documentoRepository = documentoRepository;
        this.condominioRepository = condominioRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.pessoaRepository = pessoaRepository;
        this.documentoPermissaoVisualizarRepository = documentoPermissaoVisualizarRepository;
    }

    public List<Documento> listarTodosDocumentos() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<Documento> todosOsDocumentos = documentoRepository.findAll();

        return todosOsDocumentos.stream()
                .filter(doc -> temPermissaoParaVisualizar(doc, authentication))
                .collect(Collectors.toList());
    }

    public Optional<Documento> buscarDocumentoPorId(Integer id) {
        Optional<Documento> documentoOpt = documentoRepository.findById(id);
        documentoOpt.ifPresent(doc -> {
            if (!temPermissaoParaVisualizar(doc, SecurityContextHolder.getContext().getAuthentication())) {
                throw new AccessDeniedException("Acesso negado. Você não tem permissão para visualizar este documento.");
            }
        });
        return documentoOpt;
    }

    public Documento cadastrarDocumento(Documento documento) {
        condominioRepository.findById(documento.getCondominio().getConCod()).orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado"));
        pessoaRepository.findById(documento.getUploader().getPesCod()).orElseThrow(() -> new IllegalArgumentException("Usuário de upload não encontrado"));
        if (documento.getAssembleia() != null && documento.getAssembleia().getAssCod() != null) {
            assembleiaRepository.findById(documento.getAssembleia().getAssCod()).orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada"));
        }
        documento.setDtUpload(LocalDateTime.now());
        return documentoRepository.save(documento);
    }

    public Documento atualizarDocumento(Integer id, Documento documentoAtualizado) {
        Documento documentoExistente = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado com o ID: " + id));

        if(documentoAtualizado.getNome() != null) documentoExistente.setNome(documentoAtualizado.getNome());
        if(documentoAtualizado.getTipo() != null) documentoExistente.setTipo(documentoAtualizado.getTipo());
        if(documentoAtualizado.getCaminhoArquivo() != null) documentoExistente.setCaminhoArquivo(documentoAtualizado.getCaminhoArquivo());
        if(documentoAtualizado.getPermissaoVisualizar() != null) documentoExistente.setPermissaoVisualizar(documentoAtualizado.getPermissaoVisualizar());
        
        documentoExistente.setDtAtualizacao(LocalDateTime.now());
        return documentoRepository.save(documentoExistente);
    }

    @Transactional
    public void deletarDocumento(Integer id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado com o ID: " + id));
        
        documentoPermissaoVisualizarRepository.deleteAll(documentoPermissaoVisualizarRepository.findByDocumento(documento));
        documentoRepository.delete(documento);
    }

    public boolean temPermissaoParaGerenciar(Integer condominioId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_GLOBAL_ADMIN") ||
                a.getAuthority().equals("ROLE_SINDICO_" + condominioId) ||
                a.getAuthority().equals("ROLE_ADMIN_" + condominioId));
    }

    public boolean temPermissaoParaGerenciarPorId(Integer documentoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GLOBAL_ADMIN"))) {
            return true;
        }

        Documento documento = documentoRepository.findById(documentoId)
            .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado com o ID: " + documentoId));

        if (documento.getUploader().getPesCod().equals(userDetails.getPessoa().getPesCod())) {
            return true;
        }
        
        Integer condominioId = documento.getCondominio().getConCod();

        boolean isGestorCondominio = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_SINDICO_" + condominioId) ||
                a.getAuthority().equals("ROLE_ADMIN_" + condominioId));
        if (isGestorCondominio) {
            return true;
        }
        
        if (documento.getCondominio().getAdministradora() != null) {
            Integer admCod = documento.getCondominio().getAdministradora().getAdmCod();
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GERENTE_ADMINISTRADORA_" + admCod))) {
                return true;
            }
        }

        return false;
    }
    
    private boolean temPermissaoParaVisualizar(Documento doc, Authentication auth) {
        if (temPermissaoParaGerenciar(doc.getCondominio().getConCod())) {
            return true;
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Integer pessoaId = userDetails.getPessoa().getPesCod();
        boolean isMoradorDoCondominio = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MORADOR_" + doc.getCondominio().getConCod()));

        switch (doc.getPermissaoVisualizar()) {
            case 'T': 
                return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().endsWith("_" + doc.getCondominio().getConCod()));
            case 'M': 
                return isMoradorDoCondominio;
            case 'A': 
                return false; 
            case 'S': 
                return documentoPermissaoVisualizarRepository.existsById_DocCodAndId_PesCod(doc.getDocCod(), pessoaId);
            default:
                return false;
        }
    }
}