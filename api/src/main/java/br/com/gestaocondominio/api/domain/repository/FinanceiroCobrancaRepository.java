package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinanceiroCobrancaRepository extends JpaRepository<FinanceiroCobranca, Integer> {
}