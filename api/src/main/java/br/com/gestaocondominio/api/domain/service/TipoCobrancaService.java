package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import br.com.gestaocondominio.api.domain.repository.TipoCobrancaRepository;
import br.com.gestaocondominio.api.domain.repository.FinanceiroCobrancaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TipoCobrancaService {

    private final TipoCobrancaRepository tipoCobrancaRepository;
    private final FinanceiroCobrancaRepository financeiroCobrancaRepository;

    public TipoCobrancaService(TipoCobrancaRepository tipoCobrancaRepository,
            FinanceiroCobrancaRepository financeiroCobrancaRepository) {
        this.tipoCobrancaRepository = tipoCobrancaRepository;
        this.financeiroCobrancaRepository = financeiroCobrancaRepository;
    }

    public TipoCobranca cadastrarTipoCobranca(TipoCobranca tipoCobranca) {
        if (tipoCobranca.getTicDescricao() == null || tipoCobranca.getTicDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição do tipo de cobrança não pode ser vazia.");
        }

        Optional<TipoCobranca> tipoExistente = tipoCobrancaRepository
                .findByTicDescricao(tipoCobranca.getTicDescricao());
        if (tipoExistente.isPresent()) {
            throw new IllegalArgumentException(
                    "Já existe um tipo de cobrança com esta descrição: " + tipoCobranca.getTicDescricao());
        }

        tipoCobranca.setTicDtCadastro(LocalDateTime.now());
        tipoCobranca.setTicDtAtualizacao(LocalDateTime.now());

        if (tipoCobranca.getTicAtiva() == null) {
            tipoCobranca.setTicAtiva(true);
        }

        return tipoCobrancaRepository.save(tipoCobranca);
    }

    public Optional<TipoCobranca> buscarTipoCobrancaPorId(Integer id) {
        return tipoCobrancaRepository.findById(id);
    }

    public List<TipoCobranca> listarTodosTiposCobrancaAtivos() {
        return tipoCobrancaRepository.findByTicAtiva(true);
    }

    public List<TipoCobranca> listarTodosTiposCobranca(boolean incluirInativas) {
        if (incluirInativas) {
            return tipoCobrancaRepository.findAll();
        }
        return tipoCobrancaRepository.findByTicAtiva(true);
    }

    public TipoCobranca atualizarTipoCobranca(Integer id, TipoCobranca tipoCobrancaAtualizada) {
        TipoCobranca tipoCobrancaExistente = tipoCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cobrança não encontrado com o ID: " + id));

        if (tipoCobrancaAtualizada.getTicDescricao() == null
                || tipoCobrancaAtualizada.getTicDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição do tipo de cobrança não pode ser vazia na atualização.");
        }

        if (!tipoCobrancaExistente.getTicDescricao().equalsIgnoreCase(tipoCobrancaAtualizada.getTicDescricao())) {

            Optional<TipoCobranca> tipoConflito = tipoCobrancaRepository
                    .findByTicDescricao(tipoCobrancaAtualizada.getTicDescricao());
            if (tipoConflito.isPresent() && !tipoConflito.get().getTicCod().equals(id)) {
                throw new IllegalArgumentException("Nova descrição já cadastrada para outro tipo de cobrança: "
                        + tipoCobrancaAtualizada.getTicDescricao());
            }
        }

        tipoCobrancaExistente.setTicDescricao(tipoCobrancaAtualizada.getTicDescricao());

        if (tipoCobrancaAtualizada.getTicAtiva() != null) {
            tipoCobrancaExistente.setTicAtiva(tipoCobrancaAtualizada.getTicAtiva());
        }

        tipoCobrancaExistente.setTicDtAtualizacao(LocalDateTime.now());
        return tipoCobrancaRepository.save(tipoCobrancaExistente);
    }

    public TipoCobranca inativarTipoCobranca(Integer id) {
        TipoCobranca tipo = tipoCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cobrança não encontrado com o ID: " + id));

        List<br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca> cobrancasAtivas = financeiroCobrancaRepository
                .findByTipoCobrancaAndFicStatusPagamentoNotIn(
                        tipo,
                        java.util.Arrays.asList("PAGA", "CANCELADA"));

        if (!cobrancasAtivas.isEmpty()) {
            throw new IllegalArgumentException(
                    "Não é possível inativar o tipo de cobrança, pois existem cobranças financeiras ATIVAS ou PENDENTES vinculadas a ele.");
        }

        tipo.setTicAtiva(false);
        tipo.setTicDtAtualizacao(LocalDateTime.now());
        return tipoCobrancaRepository.save(tipo);
    }

    public TipoCobranca ativarTipoCobranca(Integer id) {
        TipoCobranca tipo = tipoCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cobrança não encontrado com o ID: " + id));
        tipo.setTicAtiva(true);
        tipo.setTicDtAtualizacao(LocalDateTime.now());
        return tipoCobrancaRepository.save(tipo);
    }
}