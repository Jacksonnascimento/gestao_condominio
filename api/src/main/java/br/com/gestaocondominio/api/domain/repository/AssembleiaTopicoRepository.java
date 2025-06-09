package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AssembleiaTopico;
import br.com.gestaocondominio.api.domain.entity.Assembleia; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssembleiaTopicoRepository extends JpaRepository<AssembleiaTopico, Integer> {
    
    List<AssembleiaTopico> findByAssembleia(Assembleia assembleia); 
}