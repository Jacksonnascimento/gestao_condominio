package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Assembleia;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.enums.AssembleiaStatus;
import br.com.gestaocondominio.api.domain.repository.*;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssembleiaService {

    private final AssembleiaRepository assembleiaRepository;
    private final CondominioRepository condominioRepository;
    private final AssembleiaTopicoRepository assembleiaTopicoRepository;
    private final AssembleiaParticipanteRepository assembleiaParticipanteRepository;
    private final DocumentoRepository documentoRepository;

    public AssembleiaService(AssembleiaRepository assembleiaRepository, CondominioRepository condominioRepository, AssembleiaTopicoRepository assembleiaTopicoRepository, AssembleiaParticipanteRepository assembleiaParticipanteRepository, DocumentoRepository documentoRepository) {
        this.assembleiaRepository = assembleiaRepository;
        this.condominioRepository = condominioRepository;
        this.assembleiaTopicoRepository = assembleiaTopicoRepository;
        this.assembleiaParticipanteRepository = assembleiaParticipanteRepository;
        this.documentoRepository = documentoRepository;
    }

    public Assembleia cadastrarAssembleia(Assembleia assembleia) {
        condominioRepository.findById(assembleia.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + assembleia.getCondominio().getConCod()));

        assembleia.setAssDtCadastro(LocalDateTime.now());
        assembleia.setAssDtAtualizacao(LocalDateTime.now());
        if (assembleia.getAssStatus() == null) {
            assembleia.setAssStatus(AssembleiaStatus.AGENDADA);
        }
        if (assembleia.getAssAtiva() == null) {
            assembleia.setAssAtiva(true);
        }
        return assembleiaRepository.save(assembleia);
    }

    public List<Assembleia> listarTodasAssembleias(boolean ativas) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return ativas ? assembleiaRepository.findByAssAtiva(true) : assembleiaRepository.findAll();
        }

        Set<Integer> condoIds = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_", "ROLE_MORADOR_");
        if (condoIds.isEmpty()) {
            return List.of();
        }

        List<Condominio> condominios = condominioRepository.findAllById(condoIds);
        List<Assembleia> assembleias = assembleiaRepository.findByCondominioIn(condominios);

        if (ativas) {
            return assembleias.stream().filter(a -> a.getAssAtiva() != null && a.getAssAtiva()).collect(Collectors.toList());
        }
        return assembleias;
    }

    public Optional<Assembleia> buscarAssembleiaPorId(Integer id) {
        Optional<Assembleia> assembleiaOpt = assembleiaRepository.findById(id);
        assembleiaOpt.ifPresent(this::checkPermissionToView);
        return assembleiaOpt;
    }

    public Assembleia atualizarAssembleia(Integer id, Assembleia assembleiaAtualizada) {
        Assembleia assembleiaExistente = assembleiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + id));
        
        checkAdminOrSindicoPermission(assembleiaExistente.getCondominio().getConCod());

        assembleiaExistente.setAssDescricao(assembleiaAtualizada.getAssDescricao());
        assembleiaExistente.setAssDataHora(assembleiaAtualizada.getAssDataHora());
        if (assembleiaAtualizada.getAssStatus() != null) {
            assembleiaExistente.setAssStatus(assembleiaAtualizada.getAssStatus());
        }
        if (assembleiaAtualizada.getAssAtiva() != null) {
            assembleiaExistente.setAssAtiva(assembleiaAtualizada.getAssAtiva());
        }
        assembleiaExistente.setAssDtAtualizacao(LocalDateTime.now());
        return assembleiaRepository.save(assembleiaExistente);
    }

    public Assembleia inativarAssembleia(Integer id) {
        Assembleia assembleia = assembleiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + id));
        
        checkAdminOrSindicoPermission(assembleia.getCondominio().getConCod());
        
        if (!assembleiaTopicoRepository.findByAssembleia(assembleia).isEmpty() ||
            !assembleiaParticipanteRepository.findByAssembleia(assembleia).isEmpty() ||
            !documentoRepository.findByAssembleia(assembleia).isEmpty()) {
           throw new IllegalStateException("Não é possível inativar a assembleia, pois existem tópicos, participantes ou documentos vinculados a ela.");
        }

        assembleia.setAssAtiva(false);
        assembleia.setAssDtAtualizacao(LocalDateTime.now());
        return assembleiaRepository.save(assembleia);
    }

    public Assembleia ativarAssembleia(Integer id) {
        Assembleia assembleia = assembleiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + id));
        
        checkAdminOrSindicoPermission(assembleia.getCondominio().getConCod());

        assembleia.setAssAtiva(true);
        assembleia.setAssDtAtualizacao(LocalDateTime.now());
        return assembleiaRepository.save(assembleia);
    }
    
    private void checkAdminOrSindicoPermission(Integer condominioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean hasPermission = hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
                                hasAuthority(authentication, "ROLE_SINDICO_" + condominioId) ||
                                hasAuthority(authentication, "ROLE_ADMIN_" + condominioId);
        if (!hasPermission) {
            throw new AuthorizationDeniedException("Acesso negado. Você não tem permissão para gerenciar assembleias neste condomínio.");
        }
    }

    
    public void checkPermissionToView(Assembleia assembleia) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer condominioId = assembleia.getCondominio().getConCod();
        boolean hasPermission = hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
                                hasAuthority(authentication, "ROLE_SINDICO_" + condominioId) ||
                                hasAuthority(authentication, "ROLE_ADMIN_" + condominioId) ||
                                hasAuthority(authentication, "ROLE_MORADOR_" + condominioId);
        if (!hasPermission) {
            throw new AuthorizationDeniedException("Acesso negado. Você não tem permissão para visualizar recursos deste condomínio.");
        }
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    private Set<Integer> getCondoIdsFromRoles(Authentication auth, String... prefixes) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authString -> Arrays.stream(prefixes).anyMatch(authString::startsWith))
                .map(authString -> Integer.parseInt(authString.substring(authString.lastIndexOf('_') + 1)))
                .collect(Collectors.toSet());
    }
}