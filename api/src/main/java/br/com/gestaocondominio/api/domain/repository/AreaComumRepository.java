package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AreaComumRepository extends JpaRepository<AreaComum, Integer> {
}