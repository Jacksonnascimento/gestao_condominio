package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CondominioRepository extends JpaRepository<Condominio, Integer> {
    
}