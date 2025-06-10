package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AssembleiaVoto;
import br.com.gestaocondominio.api.domain.entity.AssembleiaVotoId;
import br.com.gestaocondominio.api.domain.entity.AssembleiaTopico; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AssembleiaVotoRepository extends JpaRepository<AssembleiaVoto, AssembleiaVotoId> {
    
    List<AssembleiaVoto> findByTopico(AssembleiaTopico topico); 
}