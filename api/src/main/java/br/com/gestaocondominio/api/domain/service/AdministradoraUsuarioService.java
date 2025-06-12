package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.AdministradoraUsuario;
import br.com.gestaocondominio.api.domain.repository.AdministradoraUsuarioRepository;
import br.com.gestaocondominio.api.domain.repository.AdministradoraRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdministradoraUsuarioService {

    private final AdministradoraUsuarioRepository administradoraUsuarioRepository;
    private final AdministradoraRepository administradoraRepository;
    private final PessoaRepository pessoaRepository;

    public AdministradoraUsuarioService(AdministradoraUsuarioRepository administradoraUsuarioRepository,
                                        AdministradoraRepository administradoraRepository,
                                        PessoaRepository pessoaRepository) {
        this.administradoraUsuarioRepository = administradoraUsuarioRepository;
        this.administradoraRepository = administradoraRepository;
        this.pessoaRepository = pessoaRepository;
    }

    public AdministradoraUsuario cadastrarAdministradoraUsuario(AdministradoraUsuario administradoraUsuario) {
      
        if (administradoraUsuario.getAdministradora() == null || administradoraUsuario.getAdministradora().getAdmCod() == null) {
            throw new IllegalArgumentException("Administradora deve ser informada para a associação.");
        }
        administradoraRepository.findById(administradoraUsuario.getAdministradora().getAdmCod())
                .orElseThrow(() -> new IllegalArgumentException("Administradora não encontrada com o ID: " + administradoraUsuario.getAdministradora().getAdmCod()));

        if (administradoraUsuario.getPessoa() == null || administradoraUsuario.getPessoa().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa deve ser informada para a associação.");
        }
        pessoaRepository.findById(administradoraUsuario.getPessoa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + administradoraUsuario.getPessoa().getPesCod()));

     
        if (administradoraUsuario.getAduPapel() == null) {
            throw new IllegalArgumentException("Papel do usuário na administradora deve ser informado.");
        }

       
        Optional<AdministradoraUsuario> associacaoExistente = administradoraUsuarioRepository.findByAdministradoraAndPessoaAndAduPapel(
            administradoraUsuario.getAdministradora(),
            administradoraUsuario.getPessoa(),
            administradoraUsuario.getAduPapel()
        );
        if (associacaoExistente.isPresent()) {
            throw new IllegalArgumentException("Esta pessoa já possui este papel nesta administradora.");
        }

        administradoraUsuario.setAduDtCadastro(LocalDateTime.now());
        administradoraUsuario.setAduDtAtualizacao(LocalDateTime.now());

        if (administradoraUsuario.getAduAtivo() == null) {
            administradoraUsuario.setAduAtivo(true); 
        }

        return administradoraUsuarioRepository.save(administradoraUsuario);
    }

    public Optional<AdministradoraUsuario> buscarAdministradoraUsuarioPorId(Integer id) {
        return administradoraUsuarioRepository.findById(id);
    }

    public List<AdministradoraUsuario> listarTodosAdministradoraUsuariosAtivos() {
        return administradoraUsuarioRepository.findByAduAtivo(true);
    }

    public List<AdministradoraUsuario> listarTodosAdministradoraUsuarios(boolean incluirInativos) {
        if (incluirInativos) {
            return administradoraUsuarioRepository.findAll();
        }
        return administradoraUsuarioRepository.findByAduAtivo(true);
    }

    public AdministradoraUsuario atualizarAdministradoraUsuario(Integer id, AdministradoraUsuario administradoraUsuarioAtualizado) {
        AdministradoraUsuario associacaoExistente = administradoraUsuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Associação de administradora a usuário não encontrada com o ID: " + id));

       
        if (administradoraUsuarioAtualizado.getAdministradora() != null && !administradoraUsuarioAtualizado.getAdministradora().getAdmCod().equals(associacaoExistente.getAdministradora().getAdmCod()) ||
            administradoraUsuarioAtualizado.getPessoa() != null && !administradoraUsuarioAtualizado.getPessoa().getPesCod().equals(associacaoExistente.getPessoa().getPesCod()) ||
            administradoraUsuarioAtualizado.getAduPapel() != null && !administradoraUsuarioAtualizado.getAduPapel().equals(associacaoExistente.getAduPapel())) {
             throw new IllegalArgumentException("Não é permitido alterar Administradora, Pessoa ou Papel de uma associação existente.");
        }

        if (administradoraUsuarioAtualizado.getAduAtivo() != null) {
            associacaoExistente.setAduAtivo(administradoraUsuarioAtualizado.getAduAtivo());
        }

        associacaoExistente.setAduDtAtualizacao(LocalDateTime.now());
        return administradoraUsuarioRepository.save(associacaoExistente);
    }

    public AdministradoraUsuario inativarAdministradoraUsuario(Integer id) {
        AdministradoraUsuario administradoraUsuario = administradoraUsuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Associação de administradora a usuário não encontrada com o ID: " + id));

       

        administradoraUsuario.setAduAtivo(false); 
        administradoraUsuario.setAduDtAtualizacao(LocalDateTime.now());
        return administradoraUsuarioRepository.save(administradoraUsuario);
    }

    public AdministradoraUsuario ativarAdministradoraUsuario(Integer id) {
        AdministradoraUsuario administradoraUsuario = administradoraUsuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Associação de administradora a usuário não encontrada com o ID: " + id));
        administradoraUsuario.setAduAtivo(true); 
        administradoraUsuario.setAduDtAtualizacao(LocalDateTime.now());
        return administradoraUsuarioRepository.save(administradoraUsuario);
    }
}