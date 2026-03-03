package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.ReservaConvidado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaConvidadoRepository extends JpaRepository<ReservaConvidado, Integer> {
}