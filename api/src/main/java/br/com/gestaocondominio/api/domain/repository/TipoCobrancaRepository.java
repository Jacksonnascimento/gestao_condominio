package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoCobrancaRepository extends JpaRepository<TipoCobranca, Integer> {
}