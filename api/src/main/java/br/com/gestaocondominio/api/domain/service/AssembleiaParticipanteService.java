package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipante;
import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipanteId;
import br.com.gestaocondominio.api.domain.repository.AssembleiaParticipanteRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssembleiaParticipanteService {

    private final AssembleiaParticipanteRepository assembleiaParticipanteRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final PessoaRepository pessoaRepository;

    public AssembleiaParticipanteService(AssembleiaParticipanteRepository assembleiaParticipanteRepository,
                                         AssembleiaRepository assembleiaRepository,
                                         PessoaRepository pessoaRepository) {
        this.assembleiaParticipanteRepository = assembleiaParticipanteRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.pessoaRepository = pessoaRepository;
    }

    public AssembleiaParticipante cadastrarAssembleiaParticipante(AssembleiaParticipante participante) {
        if (participante.getAssembleia() == null || participante.getAssembleia().getAssCod() == null) {
            throw new IllegalArgumentException("Assembleia deve ser informada para o participante.");
        }
        assembleiaRepository.findById(participante.getAssembleia().getAssCod())
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + participante.getAssembleia().getAssCod()));

        if (participante.getPessoa() == null || participante.getPessoa().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa deve ser informada para o participante.");
        }
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

    public Optional<AssembleiaParticipante> buscarAssembleiaParticipantePorId(AssembleiaParticipanteId id) {
        return assembleiaParticipanteRepository.findById(id);
    }

    public List<AssembleiaParticipante> listarTodosParticipantes() {
        return assembleiaParticipanteRepository.findAll();
    }

    public AssembleiaParticipante atualizarAssembleiaParticipante(AssembleiaParticipanteId id, AssembleiaParticipante participanteAtualizado) {
        AssembleiaParticipante participanteExistente = assembleiaParticipanteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Participante da assembleia não encontrado com o ID: " + id));

        if (!participanteAtualizado.getAssembleia().getAssCod().equals(participanteExistente.getAssembleia().getAssCod()) ||
            !participanteAtualizado.getPessoa().getPesCod().equals(participanteExistente.getPessoa().getPesCod())) {
             throw new IllegalArgumentException("Não é permitido alterar a Assembleia ou Pessoa de um participante existente.");
        }
        
        if (participanteAtualizado.getParticipacao() != null) {
            participanteExistente.setParticipacao(participanteAtualizado.getParticipacao());
        }

        return assembleiaParticipanteRepository.save(participanteExistente);
    }

    public void deletarParticipante(AssembleiaParticipanteId id) {
        assembleiaParticipanteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Participante da assembleia não encontrado para exclusão com o ID: " + id));

        assembleiaParticipanteRepository.deleteById(id);
    }
}