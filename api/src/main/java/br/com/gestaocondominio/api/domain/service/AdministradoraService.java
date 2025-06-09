package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Administradora;
import br.com.gestaocondominio.api.domain.repository.AdministradoraRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdministradoraService {

    private final AdministradoraRepository administradoraRepository;

    public AdministradoraService(AdministradoraRepository administradoraRepository) {
        this.administradoraRepository = administradoraRepository;
    }

    public Administradora cadastrarAdministradora(Administradora administradora) {
      

        administradora.setAdmDtCadastro(LocalDateTime.now());
        administradora.setAdmDtAtualizacao(LocalDateTime.now());
        return administradoraRepository.save(administradora);
    }

    public Optional<Administradora> buscarAdministradoraPorId(Integer id) {
        return administradoraRepository.findById(id);
    }

    public List<Administradora> listarTodasAdministradoras() {
        return administradoraRepository.findAll();
    }

    public Administradora atualizarAdministradora(Integer id, Administradora administradoraAtualizada) {
        Administradora administradoraExistente = administradoraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Administradora não encontrada com o ID: " + id));

        
        administradoraExistente.setDadosEmpresa(administradoraAtualizada.getDadosEmpresa());
        administradoraExistente.setResponsavel(administradoraAtualizada.getResponsavel());

        administradoraExistente.setAdmDtAtualizacao(LocalDateTime.now());
        return administradoraRepository.save(administradoraExistente);
    }
}