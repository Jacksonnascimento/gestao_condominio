package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AssembleiaVoto;
import br.com.gestaocondominio.api.domain.entity.AssembleiaVotoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssembleiaVotoRepository extends JpaRepository<AssembleiaVoto, AssembleiaVotoId> {
}