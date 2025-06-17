package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;

    public PessoaService(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    public Pessoa cadastrarPessoa(Pessoa pessoa) {
        Optional<Pessoa> pessoaExistentePorCpfCnpj = pessoaRepository.findByPesCpfCnpj(pessoa.getPesCpfCnpj());
        if (pessoaExistentePorCpfCnpj.isPresent()) {
            throw new IllegalArgumentException("CPF/CNPJ já cadastrado no sistema.");
        }

        Optional<Pessoa> pessoaExistentePorEmail = pessoaRepository.findByPesEmail(pessoa.getPesEmail());
        if (pessoaExistentePorEmail.isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        }

        pessoa.setPesDtCadastro(LocalDateTime.now());
        pessoa.setPesDtAtualizacao(LocalDateTime.now());

        if (pessoa.getPesAtivo() == null) {
            pessoa.setPesAtivo(true);
        }

      
        if (pessoa.getPesIsGlobalAdmin() == null) {
            pessoa.setPesIsGlobalAdmin(false);
        }
     

        return pessoaRepository.save(pessoa);
    }

    public Optional<Pessoa> buscarPessoaPorId(Integer id) {
        return pessoaRepository.findById(id);
    }

    public List<Pessoa> listarTodasPessoas() {
        return pessoaRepository.findAll();
    }

    public Pessoa atualizarPessoa(Integer id, Pessoa pessoaAtualizada) {
        Pessoa pessoaExistente = pessoaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + id));

        if (!pessoaExistente.getPesCpfCnpj().equals(pessoaAtualizada.getPesCpfCnpj())) {
            Optional<Pessoa> pessoaConflitoCpfCnpj = pessoaRepository.findByPesCpfCnpj(pessoaAtualizada.getPesCpfCnpj());
            if (pessoaConflitoCpfCnpj.isPresent() && !pessoaConflitoCpfCnpj.get().getPesCod().equals(id)) {
                throw new IllegalArgumentException("Novo CPF/CNPJ já cadastrado para outra pessoa.");
            }
            pessoaExistente.setPesCpfCnpj(pessoaAtualizada.getPesCpfCnpj());
        }

        if (!pessoaExistente.getPesEmail().equals(pessoaAtualizada.getPesEmail())) {
            Optional<Pessoa> pessoaConflitoEmail = pessoaRepository.findByPesEmail(pessoaAtualizada.getPesEmail());
            if (pessoaConflitoEmail.isPresent() && !pessoaConflitoEmail.get().getPesCod().equals(id)) {
                throw new IllegalArgumentException("Novo E-mail já cadastrado para outra pessoa.");
            }
            pessoaExistente.setPesEmail(pessoaAtualizada.getPesEmail());
        }

        if (pessoaAtualizada.getPesNome() != null) {
            pessoaExistente.setPesNome(pessoaAtualizada.getPesNome());
        }
        if (pessoaAtualizada.getPesTipo() != null) {
            pessoaExistente.setPesTipo(pessoaAtualizada.getPesTipo());
        }
        if (pessoaAtualizada.getPesTelefone() != null) {
            pessoaExistente.setPesTelefone(pessoaAtualizada.getPesTelefone());
        }
        if (pessoaAtualizada.getPesTelefone2() != null) {
            pessoaExistente.setPesTelefone2(pessoaAtualizada.getPesTelefone2());
        }
        if (pessoaAtualizada.getPesAtivo() != null) {
            pessoaExistente.setPesAtivo(pessoaAtualizada.getPesAtivo());
        }
        if (pessoaAtualizada.getPesSenhaLogin() != null) {
            pessoaExistente.setPesSenhaLogin(pessoaAtualizada.getPesSenhaLogin());
        }
        
     
        if (pessoaAtualizada.getPesIsGlobalAdmin() != null) {
            pessoaExistente.setPesIsGlobalAdmin(pessoaAtualizada.getPesIsGlobalAdmin());
        }

        pessoaExistente.setPesDtAtualizacao(LocalDateTime.now());
        return pessoaRepository.save(pessoaExistente);
    }

    public Pessoa inativarPessoa(Integer id) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + id));
        pessoa.setPesAtivo(false);
        pessoa.setPesDtAtualizacao(LocalDateTime.now());
        return pessoaRepository.save(pessoa);
    }

    public Pessoa ativarPessoa(Integer id) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + id));
        pessoa.setPesAtivo(true);
        pessoa.setPesDtAtualizacao(LocalDateTime.now());
        return pessoaRepository.save(pessoa);
    }
}