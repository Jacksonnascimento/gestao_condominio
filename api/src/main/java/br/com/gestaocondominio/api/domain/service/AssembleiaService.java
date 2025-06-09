package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Assembleia;

import br.com.gestaocondominio.api.domain.repository.AssembleiaRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaTopicoRepository;
import br.com.gestaocondominio.api.domain.repository.AssembleiaParticipanteRepository;
import br.com.gestaocondominio.api.domain.repository.DocumentoRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssembleiaService {

    private final AssembleiaRepository assembleiaRepository;
    private final CondominioRepository condominioRepository;
    private final AssembleiaTopicoRepository assembleiaTopicoRepository;
    private final AssembleiaParticipanteRepository assembleiaParticipanteRepository;
    private final DocumentoRepository documentoRepository;

    public AssembleiaService(AssembleiaRepository assembleiaRepository,
                             CondominioRepository condominioRepository,
                             AssembleiaTopicoRepository assembleiaTopicoRepository,
                             AssembleiaParticipanteRepository assembleiaParticipanteRepository,
                             DocumentoRepository documentoRepository) {
        this.assembleiaRepository = assembleiaRepository;
        this.condominioRepository = condominioRepository;
        this.assembleiaTopicoRepository = assembleiaTopicoRepository;
        this.assembleiaParticipanteRepository = assembleiaParticipanteRepository;
        this.documentoRepository = documentoRepository;
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
            assembleia.setAssStatus("A");
        }

        if (assembleia.getAssAtiva() == null) {
            assembleia.setAssAtiva(true);
        }

        return assembleiaRepository.save(assembleia);
    }

    public Optional<Assembleia> buscarAssembleiaPorId(Integer id) {
        return assembleiaRepository.findById(id);
    }

    public List<Assembleia> listarTodasAssembleiasAtivas() {
        return assembleiaRepository.findByAssAtiva(true);
    }

    public List<Assembleia> listarTodasAssembleias(boolean incluirInativas) {
        if (incluirInativas) {
            return assembleiaRepository.findAll();
        }
        return assembleiaRepository.findByAssAtiva(true);
    }

    public Assembleia atualizarAssembleia(Integer id, Assembleia assembleiaAtualizada) {
        Assembleia assembleiaExistente = assembleiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + id));

        if (assembleiaAtualizada.getCondominio() != null && !assembleiaAtualizada.getCondominio().getConCod().equals(assembleiaExistente.getCondominio().getConCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Condomínio de uma assembleia existente.");
        }

        assembleiaExistente.setAssDescricao(assembleiaAtualizada.getAssDescricao());
        assembleiaExistente.setAssDataHora(assembleiaAtualizada.getAssDataHora());
        assembleiaExistente.setAssStatus(assembleiaAtualizada.getAssStatus());

        // CORREÇÃO AQUI: Apenas atualiza assAtiva se um valor for explicitamente fornecido no JSON de atualização
        if (assembleiaAtualizada.getAssAtiva() != null) {
            assembleiaExistente.setAssAtiva(assembleiaAtualizada.getAssAtiva());
        }

        assembleiaExistente.setAssDtAtualizacao(LocalDateTime.now());
        return assembleiaRepository.save(assembleiaExistente);
    }

    public Assembleia inativarAssembleia(Integer id) {
        Assembleia assembleia = assembleiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + id));
        
        if (!assembleiaTopicoRepository.findByAssembleia(assembleia).isEmpty()) {
           throw new IllegalArgumentException("Não é possível inativar a assembleia, pois existem tópicos vinculados a ela.");
        }
        if (!assembleiaParticipanteRepository.findByAssembleia(assembleia).isEmpty()) {
           throw new IllegalArgumentException("Não é possível inativar a assembleia, pois existem participantes vinculados a ela.");
        }
        if (!documentoRepository.findByAssembleia(assembleia).isEmpty()) {
           throw new IllegalArgumentException("Não é possível inativar a assembleia, pois existem documentos vinculados a ela.");
        }

        assembleia.setAssAtiva(false);
        assembleia.setAssDtAtualizacao(LocalDateTime.now());
        return assembleiaRepository.save(assembleia);
    }

    public Assembleia ativarAssembleia(Integer id) {
        Assembleia assembleia = assembleiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assembleia não encontrada com o ID: " + id));
        assembleia.setAssAtiva(true);
        assembleia.setAssDtAtualizacao(LocalDateTime.now());
        return assembleiaRepository.save(assembleia);
    }
}