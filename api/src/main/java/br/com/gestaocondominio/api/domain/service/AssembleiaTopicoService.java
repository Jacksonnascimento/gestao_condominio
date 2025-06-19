package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Assembleia;
import br.com.gestaocondominio.api.domain.entity.AssembleiaTopico;
import br.com.gestaocondominio.api.domain.repository.AssembleiaRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaTopicoRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaVotoRepository;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("assembleiaTopicoService")
public class AssembleiaTopicoService {

    private final AssembleiaTopicoRepository assembleiaTopicoRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final AssembleiaVotoRepository assembleiaVotoRepository;
    private final AssembleiaService assembleiaService; // Agora é final

    
    public AssembleiaTopicoService(AssembleiaTopicoRepository assembleiaTopicoRepository,
                                   AssembleiaRepository assembleiaRepository,
                                   AssembleiaVotoRepository assembleiaVotoRepository,
                                   AssembleiaService assembleiaService) {
        this.assembleiaTopicoRepository = assembleiaTopicoRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.assembleiaVotoRepository = assembleiaVotoRepository;
        this.assembleiaService = assembleiaService;
    }

    public AssembleiaTopico cadastrarAssembleiaTopico(AssembleiaTopico topico) {
        assembleiaRepository.findById(topico.getAssembleia().getAssCod())
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + topico.getAssembleia().getAssCod()));
        return assembleiaTopicoRepository.save(topico);
    }
    
    public Optional<AssembleiaTopico> buscarTopicoPorId(Integer id) {
        Optional<AssembleiaTopico> topicoOpt = assembleiaTopicoRepository.findById(id);
        topicoOpt.ifPresent(topico -> assembleiaService.checkPermissionToView(topico.getAssembleia()));
        return topicoOpt;
    }

    public List<AssembleiaTopico> listarTopicosPorAssembleia(Integer assembleiaId) {
        Assembleia assembleia = assembleiaRepository.findById(assembleiaId)
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + assembleiaId));
        
        assembleiaService.checkPermissionToView(assembleia);

        return assembleiaTopicoRepository.findByAssembleia(assembleia);
    }
 
    public AssembleiaTopico atualizarTopico(Integer id, AssembleiaTopico topicoAtualizado) {
        AssembleiaTopico topicoExistente = assembleiaTopicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tópico de assembleia não encontrado com o ID: " + id));

        if (!temPermissaoParaGerenciarTopico(topicoExistente)) {
            throw new AuthorizationDeniedException("Acesso negado. Você não tem permissão para gerenciar este tópico.");
        }
        
        topicoExistente.setAspDescricao(topicoAtualizado.getAspDescricao());
        return assembleiaTopicoRepository.save(topicoExistente);
    }

    public void deletarTopico(Integer id) {
        AssembleiaTopico topico = assembleiaTopicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tópico de assembleia não encontrado com o ID: " + id));

        if (!temPermissaoParaGerenciarTopico(topico)) {
            throw new AuthorizationDeniedException("Acesso negado. Você não tem permissão para gerenciar este tópico.");
        }

        if (!assembleiaVotoRepository.findByTopico(topico).isEmpty()) {
            throw new IllegalStateException("Não é possível excluir o tópico pois existem votos associados a ele.");
        }

        assembleiaTopicoRepository.delete(topico);
    }
    
    public boolean temPermissaoParaGerenciarTopico(AssembleiaTopico topico) {
        Assembleia assembleia = assembleiaRepository.findById(topico.getAssembleia().getAssCod()).orElse(null);
        if (assembleia == null) return false;

        Integer condominioId = assembleia.getCondominio().getConCod();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> {
                    String authority = auth.getAuthority();
                    return authority.equals("ROLE_GLOBAL_ADMIN") ||
                           authority.equals("ROLE_SINDICO_" + condominioId) ||
                           authority.equals("ROLE_ADMIN_" + condominioId);
                });
    }
}