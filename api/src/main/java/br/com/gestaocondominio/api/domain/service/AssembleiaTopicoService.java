package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.AssembleiaTopico;
import br.com.gestaocondominio.api.domain.repository.AssembleiaTopicoRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaRepository;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;

@Service
public class AssembleiaTopicoService {

    private final AssembleiaTopicoRepository assembleiaTopicoRepository;
    private final AssembleiaRepository assembleiaRepository;

    public AssembleiaTopicoService(AssembleiaTopicoRepository assembleiaTopicoRepository,
                                   AssembleiaRepository assembleiaRepository) {
        this.assembleiaTopicoRepository = assembleiaTopicoRepository;
        this.assembleiaRepository = assembleiaRepository;
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

    public AssembleiaTopico atualizarAssembleiaTopico(Integer id, AssembleiaTopico topicoAtualizado) {
        AssembleiaTopico topicoExistente = assembleiaTopicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tópico de assembleia não encontrado com o ID: " + id));

        if (topicoAtualizado.getAssembleia() != null && !topicoAtualizado.getAssembleia().getAssCod().equals(topicoExistente.getAssembleia().getAssCod())) {
             throw new IllegalArgumentException("Não é permitido alterar a Assembleia de um tópico existente.");
        }

        topicoExistente.setAspDescricao(topicoAtualizado.getAspDescricao());
        // Se houver outros campos que possam ser atualizados no futuro, adicione-os aqui

        return assembleiaTopicoRepository.save(topicoExistente);
    }
}