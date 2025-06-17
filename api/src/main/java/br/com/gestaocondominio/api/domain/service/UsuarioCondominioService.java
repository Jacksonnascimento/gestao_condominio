package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominioId;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;
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
        Pessoa pessoa = pessoaRepository.findById(usuarioCondominio.getPessoa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + usuarioCondominio.getPessoa().getPesCod()));
        usuarioCondominio.setPessoa(pessoa);


     
        if (usuarioCondominio.getCondominio() == null || usuarioCondominio.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a associação.");
        }
        Condominio condominio = condominioRepository.findById(usuarioCondominio.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + usuarioCondominio.getCondominio().getConCod()));
        usuarioCondominio.setCondominio(condominio);

      
        if (usuarioCondominio.getUscPapel() == null) {
            throw new IllegalArgumentException("Papel do usuário no condomínio deve ser informado.");
        }

       
        usuarioCondominio.setPesCod(pessoa.getPesCod());
        usuarioCondominio.setConCod(condominio.getConCod());
      
        UsuarioCondominioId idComposto = new UsuarioCondominioId(
                usuarioCondominio.getPesCod(),
                usuarioCondominio.getConCod(),
                usuarioCondominio.getUscPapel()
        );
        if (usuarioCondominioRepository.findById(idComposto).isPresent()) {
            throw new IllegalArgumentException("Esta pessoa já possui este papel neste condomínio.");
        }

      
        if (usuarioCondominio.getUscAtivoAssociacao() == null) {
            usuarioCondominio.setUscAtivoAssociacao(true);
        }
        usuarioCondominio.setUscDtAssociacao(LocalDateTime.now());
        usuarioCondominio.setUscDtAtualizacao(LocalDateTime.now());

        return usuarioCondominioRepository.save(usuarioCondominio);
    }

    public Optional<UsuarioCondominio> buscarUsuarioCondominioPorId(UsuarioCondominioId id) {
        return usuarioCondominioRepository.findById(id);
    }

    public List<UsuarioCondominio> listarTodosUsuariosCondominio(boolean ativos) {
        if (ativos) {
            return usuarioCondominioRepository.findByUscAtivoAssociacao(true);
        }
        return usuarioCondominioRepository.findAll();
    }
    
    
    public UsuarioCondominio atualizarUsuarioCondominio(UsuarioCondominioId id, UsuarioCondominio usuarioCondominioAtualizado) {
        UsuarioCondominio usuarioCondominioExistente = usuarioCondominioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Associação de usuário a condomínio não encontrada com o ID: " + id));

    
        if (usuarioCondominioAtualizado.getUscAtivoAssociacao() != null) {
            usuarioCondominioExistente.setUscAtivoAssociacao(usuarioCondominioAtualizado.getUscAtivoAssociacao());
        }

        usuarioCondominioExistente.setUscDtAtualizacao(LocalDateTime.now());
        return usuarioCondominioRepository.save(usuarioCondominioExistente);
    }

    public UsuarioCondominio inativarUsuarioCondominio(UsuarioCondominioId id) {
        UsuarioCondominio usuarioCondominio = usuarioCondominioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Associação de usuário a condomínio não encontrada com o ID: " + id));

        usuarioCondominio.setUscAtivoAssociacao(false);
        usuarioCondominio.setUscDtAtualizacao(LocalDateTime.now());
        return usuarioCondominioRepository.save(usuarioCondominio);
    }

    public UsuarioCondominio ativarUsuarioCondominio(UsuarioCondominioId id) {
        UsuarioCondominio usuarioCondominio = usuarioCondominioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Associação de usuário a condomínio não encontrada com o ID: " + id));
        usuarioCondominio.setUscAtivoAssociacao(true);
        usuarioCondominio.setUscDtAtualizacao(LocalDateTime.now());
        return usuarioCondominioRepository.save(usuarioCondominio);
    }
}