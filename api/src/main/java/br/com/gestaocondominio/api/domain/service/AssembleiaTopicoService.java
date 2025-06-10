package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.AssembleiaTopico;
import br.com.gestaocondominio.api.domain.repository.AssembleiaTopicoRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaVotoRepository;

import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;

@Service
public class AssembleiaTopicoService {

    private final AssembleiaTopicoRepository assembleiaTopicoRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final AssembleiaVotoRepository assembleiaVotoRepository;

    public AssembleiaTopicoService(AssembleiaTopicoRepository assembleiaTopicoRepository,
                                   AssembleiaRepository assembleiaRepository,
                                   AssembleiaVotoRepository assembleiaVotoRepository) {
        this.assembleiaTopicoRepository = assembleiaTopicoRepository;
        this.assembleiaRepository = assembleiaRepository;
        this.assembleiaVotoRepository = assembleiaVotoRepository;
    }

    public AssembleiaTopico cadastrarAssembleiaTopico(AssembleiaTopico topico) {
        if (topico.getAssembleia() == null || topico.getAssembleia().getAssCod() == null) {
            throw new IllegalArgumentException("Assembleia deve ser informada para o tópico.");
        }
        assembleiaRepository.findById(topico.getAssembleia().getAssCod())
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + topico.getAssembleia().getAssCod()));

        return assembleiaTopicoRepository.save(topico);
    }

    public Optional<AssembleiaTopico> buscarAssembleiaTopicoPorId(Integer id) {
        return assembleiaTopicoRepository.findById(id);
    }

    public List<AssembleiaTopico> listarTodosTopicos() {
        return assembleiaTopicoRepository.findAll();
    }

  
    public AssembleiaTopico atualizarTopico(Integer id, AssembleiaTopico topicoAtualizado) {
        AssembleiaTopico topicoExistente = assembleiaTopicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tópico de assembleia não encontrado com o ID: " + id));

        
        if (topicoAtualizado.getAssembleia() != null && !topicoAtualizado.getAssembleia().getAssCod().equals(topicoExistente.getAssembleia().getAssCod())) {
             throw new IllegalArgumentException("Não é permitido alterar a Assembleia de um tópico existente.");
        }

      
        topicoExistente.setAspDescricao(topicoAtualizado.getAspDescricao());


        return assembleiaTopicoRepository.save(topicoExistente);
    }

    public void deletarTopico(Integer id) {
        AssembleiaTopico topico = assembleiaTopicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tópico de assembleia não encontrado com o ID: " + id));

        if (!assembleiaVotoRepository.findByTopico(topico).isEmpty()) {
            throw new IllegalArgumentException("Não é possível excluir o tópico pois existem votos associados a ele.");
        }

        assembleiaTopicoRepository.deleteById(id);
    }
}