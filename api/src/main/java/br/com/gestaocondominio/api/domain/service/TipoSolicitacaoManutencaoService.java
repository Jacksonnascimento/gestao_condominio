package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.TipoSolicitacaoManutencao;
import br.com.gestaocondominio.api.domain.repository.TipoSolicitacaoManutencaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TipoSolicitacaoManutencaoService {

    private final TipoSolicitacaoManutencaoRepository tipoSolicitacaoManutencaoRepository;

    public TipoSolicitacaoManutencaoService(TipoSolicitacaoManutencaoRepository tipoSolicitacaoManutencaoRepository) {
        this.tipoSolicitacaoManutencaoRepository = tipoSolicitacaoManutencaoRepository;
    }

    public TipoSolicitacaoManutencao cadastrarTipoSolicitacaoManutencao(TipoSolicitacaoManutencao tipoSolicitacao) {
        if (tipoSolicitacao.getTsmDescricao() == null || tipoSolicitacao.getTsmDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição do tipo de solicitação não pode ser vazia.");
        }

        Optional<TipoSolicitacaoManutencao> tipoExistente = tipoSolicitacaoManutencaoRepository.findByTsmDescricao(tipoSolicitacao.getTsmDescricao());
        if (tipoExistente.isPresent()) {
            throw new IllegalArgumentException("Já existe um tipo de solicitação com esta descrição: " + tipoSolicitacao.getTsmDescricao());
        }

        tipoSolicitacao.setTsmDtCadastro(LocalDateTime.now());
        tipoSolicitacao.setTsmDtAtualizacao(LocalDateTime.now());

        if (tipoSolicitacao.getTsmAtiva() == null) {
            tipoSolicitacao.setTsmAtiva(true); 
        }

        return tipoSolicitacaoManutencaoRepository.save(tipoSolicitacao);
    }

    public Optional<TipoSolicitacaoManutencao> buscarTipoSolicitacaoManutencaoPorId(Integer id) {
        return tipoSolicitacaoManutencaoRepository.findById(id);
    }

    public List<TipoSolicitacaoManutencao> listarTodosTiposSolicitacaoManutencaoAtivos() {
        return tipoSolicitacaoManutencaoRepository.findByTsmAtiva(true);
    }

    public List<TipoSolicitacaoManutencao> listarTodosTiposSolicitacaoManutencao(boolean incluirInativas) {
        if (incluirInativas) {
            return tipoSolicitacaoManutencaoRepository.findAll();
        }
        return tipoSolicitacaoManutencaoRepository.findByTsmAtiva(true);
    }

    public TipoSolicitacaoManutencao atualizarTipoSolicitacaoManutencao(Integer id, TipoSolicitacaoManutencao tipoSolicitacaoAtualizada) {
        TipoSolicitacaoManutencao tipoExistente = tipoSolicitacaoManutencaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de solicitação não encontrado com o ID: " + id));

        if (tipoSolicitacaoAtualizada.getTsmDescricao() == null || tipoSolicitacaoAtualizada.getTsmDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição do tipo de solicitação não pode ser vazia na atualização.");
        }

        if (!tipoSolicitacaoAtualizada.getTsmDescricao().equalsIgnoreCase(tipoExistente.getTsmDescricao())) {
            Optional<TipoSolicitacaoManutencao> tipoConflito = tipoSolicitacaoManutencaoRepository.findByTsmDescricao(tipoSolicitacaoAtualizada.getTsmDescricao());
            if (tipoConflito.isPresent() && !tipoConflito.get().getTsmCod().equals(id)) {
                throw new IllegalArgumentException("Nova descrição já cadastrada para outro tipo de solicitação: " + tipoSolicitacaoAtualizada.getTsmDescricao());
            }
        }

        tipoExistente.setTsmDescricao(tipoSolicitacaoAtualizada.getTsmDescricao());

        if (tipoSolicitacaoAtualizada.getTsmAtiva() != null) {
            tipoExistente.setTsmAtiva(tipoSolicitacaoAtualizada.getTsmAtiva());
        }

        tipoExistente.setTsmDtAtualizacao(LocalDateTime.now());
        return tipoSolicitacaoManutencaoRepository.save(tipoExistente);
    }

    public TipoSolicitacaoManutencao inativarTipoSolicitacaoManutencao(Integer id) {
        TipoSolicitacaoManutencao tipo = tipoSolicitacaoManutencaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de solicitação não encontrado com o ID: " + id));

        tipo.setTsmAtiva(false);
        tipo.setTsmDtAtualizacao(LocalDateTime.now());
        return tipoSolicitacaoManutencaoRepository.save(tipo);
    }

    public TipoSolicitacaoManutencao ativarTipoSolicitacaoManutencao(Integer id) {
        TipoSolicitacaoManutencao tipo = tipoSolicitacaoManutencaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de solicitação não encontrado com o ID: " + id));
        tipo.setTsmAtiva(true);
        tipo.setTsmDtAtualizacao(LocalDateTime.now());
        return tipoSolicitacaoManutencaoRepository.save(tipo);
    }
}