package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Morador;
import br.com.gestaocondominio.api.domain.repository.MoradorRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
public class MoradorService {

    private final MoradorRepository moradorRepository;
    private final PessoaRepository pessoaRepository;
    private final UnidadeRepository unidadeRepository;

    public MoradorService(MoradorRepository moradorRepository,
                          PessoaRepository pessoaRepository,
                          UnidadeRepository unidadeRepository) {
        this.moradorRepository = moradorRepository;
        this.pessoaRepository = pessoaRepository;
        this.unidadeRepository = unidadeRepository;
    }

    public Morador cadastrarMorador(Morador morador) {
        if (morador.getPessoa() == null || morador.getPessoa().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa deve ser informada para o morador.");
        }
        pessoaRepository.findById(morador.getPessoa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pessoa não encontrada com o ID: " + morador.getPessoa().getPesCod()));

        if (morador.getUnidade() == null || morador.getUnidade().getUniCod() == null) {
            throw new IllegalArgumentException("Unidade deve ser informada para o morador.");
        }
        unidadeRepository.findById(morador.getUnidade().getUniCod())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unidade não encontrada com o ID: " + morador.getUnidade().getUniCod()));

        Optional<Morador> moradorExistente = moradorRepository.findByPessoaAndUnidade(morador.getPessoa(),
                morador.getUnidade());
        if (moradorExistente.isPresent()) {
            throw new IllegalArgumentException("Esta pessoa já está associada a esta unidade.");
        }

        
        if (morador.getMorTipoRelacionamento() == null) {
            throw new IllegalArgumentException("Tipo de relacionamento do morador deve ser informado.");
        }
       

        morador.setMorDtCadastro(LocalDateTime.now());
        morador.setMorDtAtualizacao(LocalDateTime.now());

        return moradorRepository.save(morador);
    }

    public Optional<Morador> buscarMoradorPorId(Integer id) {
        return moradorRepository.findById(id);
    }

    public List<Morador> listarTodosMoradores() {
        return moradorRepository.findAll();
    }

    public Morador atualizarMorador(Integer id, Morador moradorAtualizado) {
        Morador moradorExistente = moradorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Morador não encontrado com o ID: " + id));

        if (moradorAtualizado.getPessoa() != null
                && !moradorAtualizado.getPessoa().getPesCod().equals(moradorExistente.getPessoa().getPesCod())) {
            throw new IllegalArgumentException(
                    "Não é permitido alterar a Pessoa de uma associação de morador existente.");
        }
        if (moradorAtualizado.getUnidade() != null
                && !moradorAtualizado.getUnidade().getUniCod().equals(moradorExistente.getUnidade().getUniCod())) {
            throw new IllegalArgumentException(
                    "Não é permitido alterar a Unidade de uma associação de morador existente.");
        }

       
        if (moradorAtualizado.getMorTipoRelacionamento() == null) {
            throw new IllegalArgumentException("Tipo de relacionamento do morador deve ser informado na atualização.");
        }
       

        moradorExistente.setMorTipoRelacionamento(moradorAtualizado.getMorTipoRelacionamento());

        moradorExistente.setMorDtAtualizacao(LocalDateTime.now());
        return moradorRepository.save(moradorExistente);
    }

    public void deletarMorador(Integer id) {
        moradorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Morador não encontrado para exclusão com o ID: " + id));
        moradorRepository.deleteById(id);
    }
}