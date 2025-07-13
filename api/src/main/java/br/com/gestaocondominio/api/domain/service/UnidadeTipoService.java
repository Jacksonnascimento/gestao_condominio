package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.UnidadeTipo;
import br.com.gestaocondominio.api.domain.repository.UnidadeTipoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UnidadeTipoService {

    private final UnidadeTipoRepository unidadeTipoRepository;

    public UnidadeTipoService(UnidadeTipoRepository unidadeTipoRepository) {
        this.unidadeTipoRepository = unidadeTipoRepository;
    }

    public UnidadeTipo criar(UnidadeTipo unidadeTipo) {
        return unidadeTipoRepository.save(unidadeTipo);
    }

    public Optional<UnidadeTipo> buscarPorId(Integer id) {
        return unidadeTipoRepository.findById(id);
    }

    public List<UnidadeTipo> listarTodos() {
        return unidadeTipoRepository.findAll();
    }

    public void deletar(Integer id) {
        unidadeTipoRepository.deleteById(id);
    }
}