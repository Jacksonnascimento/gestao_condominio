package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.SolicitacaoManutencao;
import br.com.gestaocondominio.api.domain.enums.SolicitacaoManutencaoStatus; 
import br.com.gestaocondominio.api.domain.repository.SolicitacaoManutencaoRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.TipoSolicitacaoManutencaoRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
public class SolicitacaoManutencaoService {

    private final SolicitacaoManutencaoRepository solicitacaoManutencaoRepository;
    private final CondominioRepository condominioRepository;
    private final UnidadeRepository unidadeRepository;
    private final PessoaRepository pessoaRepository;
    private final TipoSolicitacaoManutencaoRepository tipoSolicitacaoManutencaoRepository;

    public SolicitacaoManutencaoService(SolicitacaoManutencaoRepository solicitacaoManutencaoRepository,
                                        CondominioRepository condominioRepository,
                                        UnidadeRepository unidadeRepository,
                                        PessoaRepository pessoaRepository,
                                        TipoSolicitacaoManutencaoRepository tipoSolicitacaoManutencaoRepository) {
        this.solicitacaoManutencaoRepository = solicitacaoManutencaoRepository;
        this.condominioRepository = condominioRepository;
        this.unidadeRepository = unidadeRepository;
        this.pessoaRepository = pessoaRepository;
        this.tipoSolicitacaoManutencaoRepository = tipoSolicitacaoManutencaoRepository;
    }

    public SolicitacaoManutencao cadastrarSolicitacaoManutencao(SolicitacaoManutencao solicitacao) {
        if (solicitacao.getCondominio() == null || solicitacao.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a solicitação de manutenção.");
        }
        condominioRepository.findById(solicitacao.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + solicitacao.getCondominio().getConCod()));

        if (solicitacao.getUnidade() != null && solicitacao.getUnidade().getUniCod() != null) {
            unidadeRepository.findById(solicitacao.getUnidade().getUniCod())
                    .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + solicitacao.getUnidade().getUniCod()));
        }

        if (solicitacao.getSolicitante() == null || solicitacao.getSolicitante().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa solicitante deve ser informada para a solicitação de manutenção.");
        }
        pessoaRepository.findById(solicitacao.getSolicitante().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa solicitante não encontrada com o ID: " + solicitacao.getSolicitante().getPesCod()));

        if (solicitacao.getResponsavel() != null && solicitacao.getResponsavel().getPesCod() != null) {
            pessoaRepository.findById(solicitacao.getResponsavel().getPesCod())
                    .orElseThrow(() -> new IllegalArgumentException("Pessoa responsável não encontrada com o ID: " + solicitacao.getResponsavel().getPesCod()));
        }

        if (solicitacao.getTipoSolicitacao() == null || solicitacao.getTipoSolicitacao().getTsmCod() == null) {
            throw new IllegalArgumentException("Tipo de solicitação de manutenção deve ser informado.");
        }
        tipoSolicitacaoManutencaoRepository.findById(solicitacao.getTipoSolicitacao().getTsmCod())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de solicitação de manutenção não encontrado com o ID: " + solicitacao.getTipoSolicitacao().getTsmCod()));


        if (solicitacao.getDescricaoProblema() == null || solicitacao.getDescricaoProblema().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição do problema da solicitação de manutenção não pode ser vazia.");
        }

       
        if (solicitacao.getStatus() == null) {
            solicitacao.setStatus(SolicitacaoManutencaoStatus.ABERTA);
        }

        solicitacao.setDtAbertura(LocalDateTime.now());
        solicitacao.setDtAtualizacao(LocalDateTime.now());

        return solicitacaoManutencaoRepository.save(solicitacao);
    }

    public Optional<SolicitacaoManutencao> buscarSolicitacaoManutencaoPorId(Integer id) {
        return solicitacaoManutencaoRepository.findById(id);
    }

    public List<SolicitacaoManutencao> listarTodasSolicitacoesManutencao() {
        return solicitacaoManutencaoRepository.findAll();
    }

    public SolicitacaoManutencao atualizarSolicitacaoManutencao(Integer id, SolicitacaoManutencao solicitacaoAtualizada) {
        SolicitacaoManutencao solicitacaoExistente = solicitacaoManutencaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação de manutenção não encontrada com o ID: " + id));

        
        if (SolicitacaoManutencaoStatus.CONCLUIDA.equals(solicitacaoExistente.getStatus()) || SolicitacaoManutencaoStatus.CANCELADA.equals(solicitacaoExistente.getStatus())) {
            throw new IllegalArgumentException("Não é possível atualizar uma solicitação com status '" + solicitacaoExistente.getStatus() + "'.");
        }


        if (solicitacaoAtualizada.getCondominio() != null && !solicitacaoAtualizada.getCondominio().getConCod().equals(solicitacaoExistente.getCondominio().getConCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Condomínio de uma solicitação de manutenção existente.");
        }
        if (solicitacaoAtualizada.getSolicitante() != null && !solicitacaoAtualizada.getSolicitante().getPesCod().equals(solicitacaoExistente.getSolicitante().getPesCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Solicitante de uma solicitação de manutenção existente.");
        }
        if (solicitacaoAtualizada.getUnidade() != null && (solicitacaoExistente.getUnidade() == null || !solicitacaoAtualizada.getUnidade().getUniCod().equals(solicitacaoExistente.getUnidade().getUniCod()))) {
            unidadeRepository.findById(solicitacaoAtualizada.getUnidade().getUniCod())
                    .orElseThrow(() -> new IllegalArgumentException("Nova Unidade para a solicitação não encontrada com o ID: " + solicitacaoAtualizada.getUnidade().getUniCod()));
            solicitacaoExistente.setUnidade(solicitacaoAtualizada.getUnidade());
        } else if (solicitacaoAtualizada.getUnidade() == null && solicitacaoExistente.getUnidade() != null) {
            solicitacaoExistente.setUnidade(null);
        }

        if (solicitacaoAtualizada.getResponsavel() != null &&
            (solicitacaoExistente.getResponsavel() == null || !solicitacaoAtualizada.getResponsavel().getPesCod().equals(solicitacaoExistente.getResponsavel().getPesCod()))) {
            pessoaRepository.findById(solicitacaoAtualizada.getResponsavel().getPesCod())
                    .orElseThrow(() -> new IllegalArgumentException("Novo Responsável para a solicitação não encontrado com o ID: " + solicitacaoAtualizada.getResponsavel().getPesCod()));
            solicitacaoExistente.setResponsavel(solicitacaoAtualizada.getResponsavel());
        } else if (solicitacaoAtualizada.getResponsavel() == null && solicitacaoExistente.getResponsavel() != null) {
            solicitacaoExistente.setResponsavel(null);
        }

        if (solicitacaoAtualizada.getTipoSolicitacao() != null &&
            (solicitacaoExistente.getTipoSolicitacao() == null || !solicitacaoAtualizada.getTipoSolicitacao().getTsmCod().equals(solicitacaoExistente.getTipoSolicitacao().getTsmCod()))) {
            tipoSolicitacaoManutencaoRepository.findById(solicitacaoAtualizada.getTipoSolicitacao().getTsmCod())
                    .orElseThrow(() -> new IllegalArgumentException("Novo Tipo de Solicitação de Manutenção não encontrado com o ID: " + solicitacaoAtualizada.getTipoSolicitacao().getTsmCod()));
            solicitacaoExistente.setTipoSolicitacao(solicitacaoAtualizada.getTipoSolicitacao());
        } else if (solicitacaoAtualizada.getTipoSolicitacao() == null && solicitacaoExistente.getTipoSolicitacao() != null) {
             throw new IllegalArgumentException("Tipo de Solicitação de Manutenção não pode ser removido.");
        }

        if (solicitacaoAtualizada.getLocalDescricao() != null) {
            solicitacaoExistente.setLocalDescricao(solicitacaoAtualizada.getLocalDescricao());
        }
        if (solicitacaoAtualizada.getDescricaoProblema() != null) {
            solicitacaoExistente.setDescricaoProblema(solicitacaoAtualizada.getDescricaoProblema());
        }

        if (solicitacaoAtualizada.getStatus() == null) {
            throw new IllegalArgumentException("Status da solicitação de manutenção não pode ser nulo na atualização.");
        }
        
        solicitacaoExistente.setStatus(solicitacaoAtualizada.getStatus());

        if (solicitacaoAtualizada.getDtConclusao() != null) {
            solicitacaoExistente.setDtConclusao(solicitacaoAtualizada.getDtConclusao());
        }

        solicitacaoExistente.setDtAtualizacao(LocalDateTime.now());
        return solicitacaoManutencaoRepository.save(solicitacaoExistente);
    }

    public void deletarSolicitacaoManutencao(Integer id) {
        SolicitacaoManutencao solicitacao = solicitacaoManutencaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação de manutenção não encontrada para exclusão com o ID: " + id));

        if (SolicitacaoManutencaoStatus.CONCLUIDA.equals(solicitacao.getStatus()) || SolicitacaoManutencaoStatus.EM_EXECUCAO.equals(solicitacao.getStatus())) {
            throw new IllegalArgumentException("Não é possível excluir solicitações com status '" + solicitacao.getStatus() + "'.");
        }

        solicitacaoManutencaoRepository.deleteById(id);
    }
}