package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Assembleia;
import br.com.gestaocondominio.api.domain.repository.AssembleiaRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssembleiaService {

    private final AssembleiaRepository assembleiaRepository;
    private final CondominioRepository condominioRepository;

    public AssembleiaService(AssembleiaRepository assembleiaRepository, CondominioRepository condominioRepository) {
        this.assembleiaRepository = assembleiaRepository;
        this.condominioRepository = condominioRepository;
    }

    public Assembleia cadastrarAssembleia(Assembleia assembleia) {
        if (assembleia.getCondominio() == null || assembleia.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a assembleia.");
        }
        condominioRepository.findById(assembleia.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + assembleia.getCondominio().getConCod()));

        assembleia.setAssDtCadastro(LocalDateTime.now());
        assembleia.setAssDtAtualizacao(LocalDateTime.now());

        
        if (assembleia.getAssStatus() == null || assembleia.getAssStatus().trim().isEmpty()) {
            assembleia.setAssStatus("A"); // 'A' para Agendada
        }

        return assembleiaRepository.save(assembleia);
    }

    public Optional<Assembleia> buscarAssembleiaPorId(Integer id) {
        return assembleiaRepository.findById(id);
    }

    public List<Assembleia> listarTodasAssembleias() {
        return assembleiaRepository.findAll();
    }

    public Assembleia atualizarAssembleia(Integer id, Assembleia assembleiaAtualizada) {
        Assembleia assembleiaExistente = assembleiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + id));

        
        if (assembleiaAtualizada.getCondominio() != null && !assembleiaAtualizada.getCondominio().getConCod().equals(assembleiaExistente.getCondominio().getConCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o condomínio de uma assembleia existente.");
        }

        assembleiaExistente.setAssDescricao(assembleiaAtualizada.getAssDescricao());
        assembleiaExistente.setAssDataHora(assembleiaAtualizada.getAssDataHora());
        assembleiaExistente.setAssStatus(assembleiaAtualizada.getAssStatus());

        assembleiaExistente.setAssDtAtualizacao(LocalDateTime.now());
        return assembleiaRepository.save(assembleiaExistente);
    }
}