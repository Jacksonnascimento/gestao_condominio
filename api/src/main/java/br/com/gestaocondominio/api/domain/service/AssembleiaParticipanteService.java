package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Assembleia;
import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipante;
import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipanteId;
import br.com.gestaocondominio.api.domain.repository.AssembleiaParticipanteRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("assembleiaParticipanteService")
public class AssembleiaParticipanteService {

    private final AssembleiaParticipanteRepository assembleiaParticipanteRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final PessoaRepository pessoaRepository;

    @Autowired
    private AssembleiaService assembleiaService;

    public AssembleiaParticipanteService(AssembleiaParticipanteRepository assembleiaParticipanteRepository,
                                         AssembleiaRepository assembleiaRepository,
                                         PessoaRepository pessoaRepository) {
        this.assembleiaParticipanteRepository = assembleiaParticipanteRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.pessoaRepository = pessoaRepository;
    }

    public AssembleiaParticipante cadastrarAssembleiaParticipante(AssembleiaParticipante participante) {
        
        assembleiaRepository.findById(participante.getAssembleia().getAssCod())
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + participante.getAssembleia().getAssCod()));
        pessoaRepository.findById(participante.getPessoa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + participante.getPessoa().getPesCod()));

        AssembleiaParticipanteId id = new AssembleiaParticipanteId(
            participante.getAssembleia().getAssCod(),
            participante.getPessoa().getPesCod()
        );
        participante.setId(id);

        if (assembleiaParticipanteRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Esta pessoa já está registrada como participante para esta assembleia.");
        }
        
        if (participante.getParticipacao() == null) {
            participante.setParticipacao(false);
        }

        return assembleiaParticipanteRepository.save(participante);
    }
    
    public List<AssembleiaParticipante> listarParticipantesPorAssembleia(Integer assembleiaId) {
        Assembleia assembleia = assembleiaRepository.findById(assembleiaId)
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + assembleiaId));
        
        assembleiaService.checkPermissionToView(assembleia);
        return assembleiaParticipanteRepository.findByAssembleia(assembleia);
    }

    public Optional<AssembleiaParticipante> buscarAssembleiaParticipantePorId(AssembleiaParticipanteId id) {
        Optional<AssembleiaParticipante> participanteOpt = assembleiaParticipanteRepository.findById(id);
        participanteOpt.ifPresent(p -> assembleiaService.checkPermissionToView(p.getAssembleia()));
        return participanteOpt;
    }

    public AssembleiaParticipante atualizarAssembleiaParticipante(AssembleiaParticipanteId id, AssembleiaParticipante participanteAtualizado) {
        AssembleiaParticipante participanteExistente = assembleiaParticipanteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Participante da assembleia não encontrado."));

        if (!temPermissaoParaGerenciarParticipantes(id.getAssCod())) {
            throw new AccessDeniedException("Acesso negado.");
        }
        
        if (participanteAtualizado.getParticipacao() != null) {
            participanteExistente.setParticipacao(participanteAtualizado.getParticipacao());
        }

        return assembleiaParticipanteRepository.save(participanteExistente);
    }

    public void deletarParticipante(AssembleiaParticipanteId id) {
        if (!assembleiaParticipanteRepository.existsById(id)) {
            throw new IllegalArgumentException("Participante da assembleia não encontrado para exclusão.");
        }
        
        if (!temPermissaoParaGerenciarParticipantes(id.getAssCod())) {
            throw new AccessDeniedException("Acesso negado.");
        }
        
        assembleiaParticipanteRepository.deleteById(id);
    }

    public boolean temPermissaoParaGerenciarParticipantes(Integer assembleiaId) {
        Assembleia assembleia = assembleiaRepository.findById(assembleiaId).orElse(null);
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