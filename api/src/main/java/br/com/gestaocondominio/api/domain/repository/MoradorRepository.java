package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Morador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MoradorRepository extends JpaRepository<Morador, Integer> {
}