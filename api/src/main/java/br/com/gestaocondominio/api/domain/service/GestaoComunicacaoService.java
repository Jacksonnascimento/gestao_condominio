package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.GestaoComunicacao;
import br.com.gestaocondominio.api.domain.repository.GestaoComunicacaoRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GestaoComunicacaoService {

    private final GestaoComunicacaoRepository gestaoComunicacaoRepository;
    private final CondominioRepository condominioRepository;
    private final PessoaRepository pessoaRepository;

    public GestaoComunicacaoService(GestaoComunicacaoRepository gestaoComunicacaoRepository,
                                    CondominioRepository condominioRepository,
                                    PessoaRepository pessoaRepository) {
        this.gestaoComunicacaoRepository = gestaoComunicacaoRepository;
        this.condominioRepository = condominioRepository;
        this.pessoaRepository = pessoaRepository;
    }

    public GestaoComunicacao cadastrarComunicacao(GestaoComunicacao comunicacao) {
        if (comunicacao.getCondominio() == null || comunicacao.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para o comunicado.");
        }
        condominioRepository.findById(comunicacao.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + comunicacao.getCondominio().getConCod()));

        if (comunicacao.getRemetente() == null || comunicacao.getRemetente().getPesCod() == null) {
            throw new IllegalArgumentException("Remetente deve ser informado para o comunicado.");
        }
        pessoaRepository.findById(comunicacao.getRemetente().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Remetente não encontrado com o ID: " + comunicacao.getRemetente().getPesCod()));

        if (comunicacao.getComAssunto() == null || comunicacao.getComAssunto().trim().isEmpty()) {
            throw new IllegalArgumentException("Assunto do comunicado não pode ser vazio.");
        }
        if (comunicacao.getComMensagem() == null || comunicacao.getComMensagem().trim().isEmpty()) {
            throw new IllegalArgumentException("Mensagem do comunicado não pode ser vazia.");
        }

        if (comunicacao.getComDesTodos() == null) {
            comunicacao.setComDesTodos('N');
        }

        comunicacao.setComDtCadastro(LocalDateTime.now());
        comunicacao.setComDtAtualizacao(LocalDateTime.now());

        return gestaoComunicacaoRepository.save(comunicacao);
    }

    public Optional<GestaoComunicacao> buscarComunicacaoPorId(Integer id) {
        return gestaoComunicacaoRepository.findById(id);
    }

    public List<GestaoComunicacao> listarTodasComunicacoes() {
        return gestaoComunicacaoRepository.findAll();
    }

    public GestaoComunicacao atualizarComunicacao(Integer id, GestaoComunicacao comunicacaoAtualizada) {
        GestaoComunicacao comunicacaoExistente = gestaoComunicacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comunicado não encontrado com o ID: " + id));

        if (comunicacaoAtualizada.getCondominio() != null && !comunicacaoAtualizada.getCondominio().getConCod().equals(comunicacaoExistente.getCondominio().getConCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Condomínio de um comunicado existente.");
        }
        if (comunicacaoAtualizada.getRemetente() != null && !comunicacaoAtualizada.getRemetente().getPesCod().equals(comunicacaoExistente.getRemetente().getPesCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Remetente de um comunicado existente.");
        }

        comunicacaoExistente.setComAssunto(comunicacaoAtualizada.getComAssunto());
        comunicacaoExistente.setComMensagem(comunicacaoAtualizada.getComMensagem());
        comunicacaoExistente.setComDesTodos(comunicacaoAtualizada.getComDesTodos());
        comunicacaoExistente.setComTipoNotificacao(comunicacaoAtualizada.getComTipoNotificacao());
        comunicacaoExistente.setComDtEnvio(comunicacaoAtualizada.getComDtEnvio());

        comunicacaoExistente.setComDtAtualizacao(LocalDateTime.now());
        return gestaoComunicacaoRepository.save(comunicacaoExistente);
    }
}