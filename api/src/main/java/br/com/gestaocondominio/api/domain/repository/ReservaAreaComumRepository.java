package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.ReservaAreaComum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaAreaComumRepository extends JpaRepository<ReservaAreaComum, Integer> {
    
}