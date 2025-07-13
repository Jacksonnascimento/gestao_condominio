package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.TaxaCondominioValor;
import br.com.gestaocondominio.api.domain.repository.TaxaCondominioValorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TaxaCondominioValorService {

    private final TaxaCondominioValorRepository taxaCondominioValorRepository;

    public TaxaCondominioValorService(TaxaCondominioValorRepository taxaCondominioValorRepository) {
        this.taxaCondominioValorRepository = taxaCondominioValorRepository;
    }

    public TaxaCondominioValor salvar(TaxaCondominioValor taxaValor) {
        return taxaCondominioValorRepository.save(taxaValor);
    }

    public Optional<TaxaCondominioValor> buscarPorId(Integer id) {
        return taxaCondominioValorRepository.findById(id);
    }

    public List<TaxaCondominioValor> listarTodos() {
        return taxaCondominioValorRepository.findAll();
    }

    public void deletar(Integer id) {
        taxaCondominioValorRepository.deleteById(id);
    }
}