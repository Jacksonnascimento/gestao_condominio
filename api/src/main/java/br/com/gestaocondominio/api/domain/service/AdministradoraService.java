package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Administradora;
import br.com.gestaocondominio.api.domain.repository.AdministradoraRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository; 
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdministradoraService {

    private final AdministradoraRepository administradoraRepository;
    private final PessoaRepository pessoaRepository; // Adicione esta linha

    public AdministradoraService(AdministradoraRepository administradoraRepository,
                                 PessoaRepository pessoaRepository) { // Adicione PessoaRepository ao construtor
        this.administradoraRepository = administradoraRepository;
        this.pessoaRepository = pessoaRepository; // Atribua
    }

    public Administradora cadastrarAdministradora(Administradora administradora) {
        if (administradora.getDadosEmpresa() == null || administradora.getDadosEmpresa().getPesCod() == null) {
            throw new IllegalArgumentException("Dados da Empresa (Pessoa) devem ser informados para a administradora.");
        }
        // Validação da existência da Pessoa 'dadosEmpresa'
        pessoaRepository.findById(administradora.getDadosEmpresa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa (Dados da Empresa) não encontrada com o ID: " + administradora.getDadosEmpresa().getPesCod()));


        if (administradora.getResponsavel() == null || administradora.getResponsavel().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa Responsável deve ser informada para a administradora.");
        }
        // Validação da existência da Pessoa 'responsavel'
        pessoaRepository.findById(administradora.getResponsavel().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa (Responsável) não encontrada com o ID: " + administradora.getResponsavel().getPesCod()));


        administradora.setAdmDtCadastro(LocalDateTime.now());
        administradora.setAdmDtAtualizacao(LocalDateTime.now());

        if (administradora.getAdmAtivo() == null) {
            administradora.setAdmAtivo(true);
        }

        return administradoraRepository.save(administradora);
    }

    public Optional<Administradora> buscarAdministradoraPorId(Integer id) {
        return administradoraRepository.findById(id);
    }

    public List<Administradora> listarTodasAdministradorasAtivas() {
        return administradoraRepository.findByAdmAtivo(true);
    }

    public List<Administradora> listarTodasAdministradoras(boolean incluirInativas) {
        if (incluirInativas) {
            return administradoraRepository.findAll();
        }
        return administradoraRepository.findByAdmAtivo(true);
    }

    public Administradora atualizarAdministradora(Integer id, Administradora administradoraAtualizada) {
        Administradora administradoraExistente = administradoraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Administradora não encontrada com o ID: " + id));

        // Validação e atualização de 'dadosEmpresa'
        if (administradoraAtualizada.getDadosEmpresa() != null && !administradoraAtualizada.getDadosEmpresa().getPesCod().equals(administradoraExistente.getDadosEmpresa().getPesCod())) {
             pessoaRepository.findById(administradoraAtualizada.getDadosEmpresa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Nova Pessoa (Dados da Empresa) não encontrada com o ID: " + administradoraAtualizada.getDadosEmpresa().getPesCod()));
             administradoraExistente.setDadosEmpresa(administradoraAtualizada.getDadosEmpresa());
        } else if (administradoraAtualizada.getDadosEmpresa() == null) {
            throw new IllegalArgumentException("Dados da Empresa (Pessoa) não podem ser nulos na atualização.");
        }

        // Validação e atualização de 'responsavel'
        if (administradoraAtualizada.getResponsavel() != null && !administradoraAtualizada.getResponsavel().getPesCod().equals(administradoraExistente.getResponsavel().getPesCod())) {
             pessoaRepository.findById(administradoraAtualizada.getResponsavel().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Nova Pessoa (Responsável) não encontrada com o ID: " + administradoraAtualizada.getResponsavel().getPesCod()));
             administradoraExistente.setResponsavel(administradoraAtualizada.getResponsavel());
        } else if (administradoraAtualizada.getResponsavel() == null) {
            throw new IllegalArgumentException("Pessoa Responsável não pode ser nula na atualização.");
        }

        administradoraExistente.setAdmAtivo(administradoraAtualizada.getAdmAtivo());

        administradoraExistente.setAdmDtAtualizacao(LocalDateTime.now());
        return administradoraRepository.save(administradoraExistente);
    }

    public Administradora inativarAdministradora(Integer id) {
        Administradora administradora = administradoraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Administradora não encontrada com o ID: " + id));
        administradora.setAdmAtivo(false);
        administradora.setAdmDtAtualizacao(LocalDateTime.now());
        return administradoraRepository.save(administradora);
    }

    public Administradora ativarAdministradora(Integer id) {
        Administradora administradora = administradoraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Administradora não encontrada com o ID: " + id));
        administradora.setAdmAtivo(true);
        administradora.setAdmDtAtualizacao(LocalDateTime.now());
        return administradoraRepository.save(administradora);
    }
}