package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import br.com.gestaocondominio.api.domain.repository.TipoCobrancaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TipoCobrancaService {

    private final TipoCobrancaRepository tipoCobrancaRepository;

    public TipoCobrancaService(TipoCobrancaRepository tipoCobrancaRepository) {
        this.tipoCobrancaRepository = tipoCobrancaRepository;
    }

    public TipoCobranca cadastrarTipoCobranca(TipoCobranca tipoCobranca) {
        if (tipoCobranca.getTicDescricao() == null || tipoCobranca.getTicDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição do tipo de cobrança não pode ser vazia.");
        }

      
        Optional<TipoCobranca> tipoExistente = tipoCobrancaRepository.findByTicDescricao(tipoCobranca.getTicDescricao());
        if (tipoExistente.isPresent()) {
            throw new IllegalArgumentException("Já existe um tipo de cobrança com esta descrição: " + tipoCobranca.getTicDescricao());
        }

        tipoCobranca.setTicDtCadastro(LocalDateTime.now());
        tipoCobranca.setTicDtAtualizacao(LocalDateTime.now());

        return tipoCobrancaRepository.save(tipoCobranca);
    }

    public Optional<TipoCobranca> buscarTipoCobrancaPorId(Integer id) {
        return tipoCobrancaRepository.findById(id);
    }

    public List<TipoCobranca> listarTodosTiposCobranca() {
        return tipoCobrancaRepository.findAll();
    }

    public TipoCobranca atualizarTipoCobranca(Integer id, TipoCobranca tipoCobrancaAtualizada) {
        TipoCobranca tipoCobrancaExistente = tipoCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cobrança não encontrado com o ID: " + id));

        if (tipoCobrancaAtualizada.getTicDescricao() == null || tipoCobrancaAtualizada.getTicDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição do tipo de cobrança não pode ser vazia na atualização.");
        }

        
        if (!tipoCobrancaExistente.getTicDescricao().equalsIgnoreCase(tipoCobrancaAtualizada.getTicDescricao())) { 
            Optional<TipoCobranca> tipoConflito = tipoCobrancaRepository.findByTicDescricao(tipoCobrancaAtualizada.getTicDescricao());
            if (tipoConflito.isPresent() && !tipoConflito.get().getTicCod().equals(id)) {
                throw new IllegalArgumentException("Nova descrição já cadastrada para outro tipo de cobrança: " + tipoCobrancaAtualizada.getTicDescricao());
            }
        }

        tipoCobrancaExistente.setTicDescricao(tipoCobrancaAtualizada.getTicDescricao());
        tipoCobrancaExistente.setTicDtAtualizacao(LocalDateTime.now());

        return tipoCobrancaRepository.save(tipoCobrancaExistente);
    }
}