package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.ComunicadoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComunicadoEntregaRepository extends JpaRepository<ComunicadoEntrega, Integer> {
}