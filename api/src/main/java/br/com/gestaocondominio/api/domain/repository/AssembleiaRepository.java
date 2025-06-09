package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Assembleia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AssembleiaRepository extends JpaRepository<Assembleia, Integer> {
    
    List<Assembleia> findByAssAtiva(Boolean assAtiva);
}