package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Condominio;

import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.AdministradoraRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CondominioService {

    private final CondominioRepository condominioRepository;
    private final AdministradoraRepository administradoraRepository;

    public CondominioService(CondominioRepository condominioRepository, AdministradoraRepository administradoraRepository) {
        this.condominioRepository = condominioRepository;
        this.administradoraRepository = administradoraRepository;
    }

    public Condominio cadastrarCondominio(Condominio condominio) {
        if (condominio.getAdministradora() == null || condominio.getAdministradora().getAdmCod() == null) {
            throw new IllegalArgumentException("Administradora deve ser informada para o condomínio.");
        }
        administradoraRepository.findById(condominio.getAdministradora().getAdmCod())
                .orElseThrow(() -> new IllegalArgumentException("Administradora não encontrada com o ID: " + condominio.getAdministradora().getAdmCod()));

      

        if (condominio.getConPais() == null || condominio.getConPais().trim().isEmpty()) {
            condominio.setConPais("Brasil");
        }

        condominio.setConDtCadastro(LocalDateTime.now());
        condominio.setConDtAtualizacao(LocalDateTime.now());

        return condominioRepository.save(condominio);
    }

    public Optional<Condominio> buscarCondominioPorId(Integer id) {
        return condominioRepository.findById(id);
    }

    public List<Condominio> listarTodosCondominios() {
        return condominioRepository.findAll();
    }

    public Condominio atualizarCondominio(Integer id, Condominio condominioAtualizado) {
        Condominio condominioExistente = condominioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + id));

        
        if (condominioAtualizado.getAdministradora() != null &&
            !condominioAtualizado.getAdministradora().getAdmCod().equals(condominioExistente.getAdministradora().getAdmCod())) {
            administradoraRepository.findById(condominioAtualizado.getAdministradora().getAdmCod())
                .orElseThrow(() -> new IllegalArgumentException("Nova Administradora não encontrada com o ID: " + condominioAtualizado.getAdministradora().getAdmCod()));
            condominioExistente.setAdministradora(condominioAtualizado.getAdministradora());
        }

       
        condominioExistente.setConNome(condominioAtualizado.getConNome());
        condominioExistente.setConLogradouro(condominioAtualizado.getConLogradouro());
        condominioExistente.setConNumero(condominioAtualizado.getConNumero());
        condominioExistente.setConComplemento(condominioAtualizado.getConComplemento());
        condominioExistente.setConBairro(condominioAtualizado.getConBairro());
        condominioExistente.setConCidade(condominioAtualizado.getConCidade());
        condominioExistente.setConEstado(condominioAtualizado.getConEstado());
        condominioExistente.setConCep(condominioAtualizado.getConCep());
        condominioExistente.setConPais(condominioAtualizado.getConPais());
        condominioExistente.setConReferencia(condominioAtualizado.getConReferencia());
        condominioExistente.setConNumeroUnidades(condominioAtualizado.getConNumeroUnidades());
        condominioExistente.setConTipologia(condominioAtualizado.getConTipologia());
        condominioExistente.setConDtVencimentoTaxa(condominioAtualizado.getConDtVencimentoTaxa());

        condominioExistente.setConDtAtualizacao(LocalDateTime.now());
        return condominioRepository.save(condominioExistente);
    }
}