package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.VotacaoResultadoDTO;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.VotoOpcao;
import br.com.gestaocondominio.api.domain.repository.AssembleiaParticipanteRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaTopicoRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaVotoRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssembleiaVotoService {

    private final AssembleiaVotoRepository assembleiaVotoRepository;
    private final AssembleiaTopicoRepository assembleiaTopicoRepository;
    private final PessoaRepository pessoaRepository;
    private final AssembleiaParticipanteRepository assembleiaParticipanteRepository;
    private final AssembleiaService assembleiaService;

    public AssembleiaVotoService(AssembleiaVotoRepository assembleiaVotoRepository, AssembleiaTopicoRepository assembleiaTopicoRepository, PessoaRepository pessoaRepository, AssembleiaParticipanteRepository assembleiaParticipanteRepository, AssembleiaService assembleiaService) {
        this.assembleiaVotoRepository = assembleiaVotoRepository;
        this.assembleiaTopicoRepository = assembleiaTopicoRepository;
        this.pessoaRepository = pessoaRepository;
        this.assembleiaParticipanteRepository = assembleiaParticipanteRepository;
        this.assembleiaService = assembleiaService;
    }

    public AssembleiaVoto cadastrarAssembleiaVoto(AssembleiaVoto voto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        AssembleiaTopico topico = assembleiaTopicoRepository.findById(voto.getTopico().getAspCod())
                .orElseThrow(() -> new IllegalArgumentException("Tópico da assembleia não encontrado."));
        Pessoa pessoaVotante = pessoaRepository.findById(voto.getPessoa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada."));

        if (!userDetails.getPessoa().getPesCod().equals(pessoaVotante.getPesCod())) {
            throw new AccessDeniedException("Um usuário não pode votar em nome de outro.");
        }

        AssembleiaParticipanteId participanteId = new AssembleiaParticipanteId(topico.getAssembleia().getAssCod(), pessoaVotante.getPesCod());
        assembleiaParticipanteRepository.findById(participanteId)
                .orElseThrow(() -> new AccessDeniedException("Acesso negado. Você não é um participante desta assembleia."));

        AssembleiaVotoId id = new AssembleiaVotoId(topico.getAspCod(), pessoaVotante.getPesCod());
        if (assembleiaVotoRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Esta pessoa já votou neste tópico da assembleia.");
        }
        voto.setId(id);
        
        return assembleiaVotoRepository.save(voto);
    }

    public Object listarVotosPorTopico(Integer topicoId) {
        AssembleiaTopico topico = assembleiaTopicoRepository.findById(topicoId)
                .orElseThrow(() -> new IllegalArgumentException("Tópico não encontrado com o ID: " + topicoId));

        assembleiaService.checkPermissionToView(topico.getAssembleia());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Integer condominioId = topico.getAssembleia().getCondominio().getConCod();

        boolean isGestor = authentication.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_GLOBAL_ADMIN") ||
                a.getAuthority().equals("ROLE_SINDICO_" + condominioId) ||
                a.getAuthority().equals("ROLE_ADMIN_" + condominioId)
        );

        List<AssembleiaVoto> votos = assembleiaVotoRepository.findByTopico(topico);

        if (isGestor) {
            return votos;
        } else {
            long votosSim = votos.stream().filter(v -> v.getVoto() == VotoOpcao.SIM).count();
            long votosNao = votos.stream().filter(v -> v.getVoto() == VotoOpcao.NAO).count();
            long votosAbstencao = votos.stream().filter(v -> v.getVoto() == VotoOpcao.ABSTENCAO).count();
            long total = votos.size();

            return new VotacaoResultadoDTO(votosSim, votosNao, votosAbstencao, total);
        }
    }

    public Optional<AssembleiaVoto> buscarVotoPorId(AssembleiaVotoId id) {
        Optional<AssembleiaVoto> votoOpt = assembleiaVotoRepository.findById(id);
        votoOpt.ifPresent(v -> assembleiaService.checkPermissionToView(v.getTopico().getAssembleia()));
        return votoOpt;
    }

    public AssembleiaVoto atualizarAssembleiaVoto(AssembleiaVotoId id, AssembleiaVoto votoAtualizado) {
        AssembleiaVoto votoExistente = assembleiaVotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voto não encontrado."));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        if (!userDetails.getPessoa().getPesCod().equals(votoExistente.getPessoa().getPesCod())) {
            throw new AccessDeniedException("Acesso negado. Você não pode alterar o voto de outra pessoa.");
        }

        votoExistente.setVoto(votoAtualizado.getVoto());
        return assembleiaVotoRepository.save(votoExistente);
    }

    public void deletarVoto(AssembleiaVotoId id) {
        AssembleiaVoto voto = assembleiaVotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voto não encontrado."));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Integer condominioId = voto.getTopico().getAssembleia().getCondominio().getConCod();

        boolean isOwner = userDetails.getPessoa().getPesCod().equals(voto.getPessoa().getPesCod());
        boolean isGestor = authentication.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_GLOBAL_ADMIN") ||
                a.getAuthority().equals("ROLE_SINDICO_" + condominioId) ||
                a.getAuthority().equals("ROLE_ADMIN_" + condominioId)
        );

        if (!isOwner && !isGestor) {
            throw new AccessDeniedException("Acesso negado. Você não tem permissão para deletar este voto.");
        }

        assembleiaVotoRepository.delete(voto);
    }
}