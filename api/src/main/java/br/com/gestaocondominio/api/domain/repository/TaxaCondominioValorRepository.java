package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.TaxaCondominioValor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxaCondominioValorRepository extends JpaRepository<TaxaCondominioValor, Integer> {
}