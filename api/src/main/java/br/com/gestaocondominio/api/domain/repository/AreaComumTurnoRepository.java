package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AreaComumTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaComumTurnoRepository extends JpaRepository<AreaComumTurno, Integer> {
    List<AreaComumTurno> findByAreaComumAreCodAndAtivoTrueOrderByHoraInicioAsc(Integer areCod);
}