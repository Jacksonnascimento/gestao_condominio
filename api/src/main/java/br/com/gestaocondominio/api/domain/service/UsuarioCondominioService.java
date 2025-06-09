package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominioId;
import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioCondominioService {

    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final PessoaRepository pessoaRepository;
    private final CondominioRepository condominioRepository;

    public UsuarioCondominioService(UsuarioCondominioRepository usuarioCondominioRepository,
                                    PessoaRepository pessoaRepository,
                                    CondominioRepository condominioRepository) {
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.pessoaRepository = pessoaRepository;
        this.condominioRepository = condominioRepository;
    }

    public UsuarioCondominio cadastrarUsuarioCondominio(UsuarioCondominio usuarioCondominio) {
        if (usuarioCondominio.getPessoa() == null || usuarioCondominio.getPessoa().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa deve ser informada para a associação.");
        }
        pessoaRepository.findById(usuarioCondominio.getPessoa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + usuarioCondominio.getPessoa().getPesCod()));

        if (usuarioCondominio.getCondominio() == null || usuarioCondominio.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a associação.");
        }
        condominioRepository.findById(usuarioCondominio.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + usuarioCondominio.getCondominio().getConCod()));

        if (usuarioCondominio.getId() == null || usuarioCondominio.getId().getUscPapel() == null || String.valueOf(usuarioCondominio.getId().getUscPapel()).trim().isEmpty()) {
            throw new IllegalArgumentException("Papel do usuário no condomínio deve ser informado.");
        }
        char papel = Character.toUpperCase(usuarioCondominio.getId().getUscPapel());
        if (papel != 'S' && papel != 'M' && papel != 'F' && papel != 'P' && papel != 'A') {
            throw new IllegalArgumentException("Papel inválido. Use 'S', 'M', 'F', 'P' ou 'A'.");
        }
        usuarioCondominio.getId().setUscPapel(papel);

        UsuarioCondominioId idComposto = new UsuarioCondominioId(
            usuarioCondominio.getPessoa().getPesCod(),
            usuarioCondominio.getCondominio().getConCod(),
            usuarioCondominio.getId().getUscPapel()
        );
        usuarioCondominio.setId(idComposto);

        if (usuarioCondominioRepository.findById(idComposto).isPresent()) {
            throw new IllegalArgumentException("Esta pessoa já possui este papel neste condomínio.");
        }

        if (usuarioCondominio.getUscAtivoAssociacao() == null) {
            usuarioCondominio.setUscAtivoAssociacao(true);
        }

        usuarioCondominio.setUscDtAssociacao(LocalDateTime.now());
        usuarioCondominio.setUscDtAtualizacao(LocalDateTime.now()); // <-- NOVA LINHA

        return usuarioCondominioRepository.save(usuarioCondominio);
    }

    public Optional<UsuarioCondominio> buscarUsuarioCondominioPorId(UsuarioCondominioId id) {
        return usuarioCondominioRepository.findById(id);
    }

    public List<UsuarioCondominio> listarTodosUsuariosCondominio() {
        return usuarioCondominioRepository.findAll();
    }

    public UsuarioCondominio atualizarUsuarioCondominio(UsuarioCondominioId id, UsuarioCondominio usuarioCondominioAtualizado) {
        UsuarioCondominio usuarioCondominioExistente = usuarioCondominioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Associação de usuário a condomínio não encontrada com o ID: " + id));

        if (!usuarioCondominioAtualizado.getId().getPesCod().equals(usuarioCondominioExistente.getId().getPesCod()) ||
            !usuarioCondominioAtualizado.getId().getConCod().equals(usuarioCondominioExistente.getId().getConCod()) ||
            !usuarioCondominioAtualizado.getId().getUscPapel().equals(usuarioCondominioExistente.getId().getUscPapel())) {
             throw new IllegalArgumentException("Não é permitido alterar Pessoa, Condomínio ou Papel de uma associação existente.");
        }
        
        usuarioCondominioExistente.setUscAtivoAssociacao(usuarioCondominioAtualizado.getUscAtivoAssociacao());
        usuarioCondominioExistente.setUscDtAtualizacao(LocalDateTime.now()); // <-- NOVA LINHA

        return usuarioCondominioRepository.save(usuarioCondominioExistente);
    }
}