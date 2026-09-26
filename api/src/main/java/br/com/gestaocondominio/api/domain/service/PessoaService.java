package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.PessoaUpdateRequest;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.util.ValidadorDocumento;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;

    public PessoaService(PessoaRepository pessoaRepository, PasswordEncoder passwordEncoder) {
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<Pessoa> buscarPorCpfCnpj(String cpfCnpj) {
        return pessoaRepository.findByPesCpfCnpj(cpfCnpj);
    }

    public Pessoa cadastrarPessoa(Pessoa pessoa) {
        if (!ValidadorDocumento.isValid(pessoa.getPesCpfCnpj())) {
            throw new IllegalArgumentException("O CPF/CNPJ informado é inválido.");
        }

        String documentoNumerico = pessoa.getPesCpfCnpj().replaceAll("[^0-9]", "");
        if (pessoa.getPesTipo() == 'F' && documentoNumerico.length() == 14) {
            throw new IllegalArgumentException("CNPJ não pode ser cadastrado para pessoa física.");
        }
        if (pessoa.getPesTipo() == 'J' && documentoNumerico.length() == 11) {
            throw new IllegalArgumentException("CPF não pode ser cadastrado para pessoa jurídica.");
        }

        pessoaRepository.findByPesCpfCnpj(pessoa.getPesCpfCnpj()).ifPresent(p -> {
            throw new IllegalArgumentException("CPF/CNPJ já cadastrado no sistema.");
        });
        pessoaRepository.findByPesEmail(pessoa.getPesEmail()).ifPresent(p -> {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        });

        if (StringUtils.hasText(pessoa.getPesSenhaLogin())) {
            pessoa.setPesSenhaLogin(passwordEncoder.encode(pessoa.getPesSenhaLogin()));
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

    @Transactional
    public Pessoa atualizarPessoa(Integer id, PessoaUpdateRequest dadosParaAtualizar) {
        Pessoa pessoaNoBanco = pessoaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));

        if (dadosParaAtualizar.pesNome() != null) {
            pessoaNoBanco.setPesNome(dadosParaAtualizar.pesNome());
        }
        if (dadosParaAtualizar.pesTipo() != null) {
            pessoaNoBanco.setPesTipo(dadosParaAtualizar.pesTipo());
        }
        if (dadosParaAtualizar.pesTelefone() != null) {
            pessoaNoBanco.setPesTelefone(dadosParaAtualizar.pesTelefone());
        }
        if (dadosParaAtualizar.pesTelefone2() != null) {
            pessoaNoBanco.setPesTelefone2(dadosParaAtualizar.pesTelefone2());
        }

        if (dadosParaAtualizar.pesCpfCnpj() != null
                && !pessoaNoBanco.getPesCpfCnpj().equals(dadosParaAtualizar.pesCpfCnpj())) {
            if (!ValidadorDocumento.isValid(dadosParaAtualizar.pesCpfCnpj())) {
                throw new IllegalArgumentException("O novo CPF/CNPJ informado é inválido.");
            }
            pessoaRepository.findByPesCpfCnpj(dadosParaAtualizar.pesCpfCnpj()).ifPresent(p -> {
                if (!p.getPesCod().equals(id))
                    throw new IllegalArgumentException("CPF/CNPJ já cadastrado para outra pessoa.");
            });
            pessoaNoBanco.setPesCpfCnpj(dadosParaAtualizar.pesCpfCnpj());
        }

        Character tipoPessoaFinal = (dadosParaAtualizar.pesTipo() != null) ? dadosParaAtualizar.pesTipo()
                : pessoaNoBanco.getPesTipo();
        String documentoFinal = (dadosParaAtualizar.pesCpfCnpj() != null) ? dadosParaAtualizar.pesCpfCnpj()
                : pessoaNoBanco.getPesCpfCnpj();
        String documentoNumerico = documentoFinal.replaceAll("[^0-9]", "");

        if (tipoPessoaFinal == 'F' && documentoNumerico.length() == 14) {
            throw new IllegalArgumentException("CNPJ não pode ser cadastrado para pessoa física.");
        }
        if (tipoPessoaFinal == 'J' && documentoNumerico.length() == 11) {
            throw new IllegalArgumentException("CPF não pode ser cadastrado para pessoa jurídica.");
        }

        if (dadosParaAtualizar.pesEmail() != null
                && !pessoaNoBanco.getPesEmail().equals(dadosParaAtualizar.pesEmail())) {
            pessoaRepository.findByPesEmail(dadosParaAtualizar.pesEmail()).ifPresent(p -> {
                if (!p.getPesCod().equals(id))
                    throw new IllegalArgumentException("E-mail já cadastrado para outra pessoa.");
            });
            pessoaNoBanco.setPesEmail(dadosParaAtualizar.pesEmail());
        }

        if (StringUtils.hasText(dadosParaAtualizar.pesSenhaLogin())) {
            pessoaNoBanco.setPesSenhaLogin(passwordEncoder.encode(dadosParaAtualizar.pesSenhaLogin()));
        }

        if (StringUtils.hasText(dadosParaAtualizar.pesImagem())) {
            pessoaNoBanco.setPesImagem(Base64.getDecoder().decode(dadosParaAtualizar.pesImagem()));
        }

        pessoaNoBanco.setPesDtAtualizacao(LocalDateTime.now());
        return pessoaRepository.save(pessoaNoBanco);
    }

    @Transactional
    public void atualizarSenha(Integer id, String senhaAtual, String novaSenha) {
        Pessoa pessoaNoBanco = pessoaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));

        if (!passwordEncoder.matches(senhaAtual, pessoaNoBanco.getPesSenhaLogin())) {
            throw new BadCredentialsException("A senha atual informada está incorreta.");
        }

        if (!StringUtils.hasText(novaSenha)) {
            throw new IllegalArgumentException("A nova senha não pode estar em branco.");
        }

        pessoaNoBanco.setPesSenhaLogin(passwordEncoder.encode(novaSenha));
        pessoaNoBanco.setPesDtAtualizacao(LocalDateTime.now());
        pessoaRepository.save(pessoaNoBanco);
    }

    /**
     * Define a senha sem conferir a atual, para a administração de usuários. Como a senha muda, os tokens já emitidos
     * para a pessoa deixam de valer.
     */
    @Transactional
    public void definirSenha(Integer id, String novaSenha) {
        Pessoa pessoaNoBanco = pessoaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));

        if (!StringUtils.hasText(novaSenha)) {
            throw new IllegalArgumentException("A nova senha não pode estar em branco.");
        }

        pessoaNoBanco.setPesSenhaLogin(passwordEncoder.encode(novaSenha));
        pessoaNoBanco.setPesDtAtualizacao(LocalDateTime.now());
        pessoaRepository.save(pessoaNoBanco);
    }

    /** Troca a foto da pessoa pela imagem já conferida; {@code null} tira a foto. */
    @Transactional
    public Pessoa atualizarFoto(Integer id, byte[] imagem) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));
        pessoa.setPesImagem(imagem);
        pessoa.setPesDtAtualizacao(LocalDateTime.now());
        return pessoaRepository.save(pessoa);
    }

    public Pessoa getLoggedInUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return pessoaRepository.findByPesEmail(username)
                .orElseThrow(() -> new IllegalStateException("Usuário logado não encontrado no banco de dados."));
    }
}