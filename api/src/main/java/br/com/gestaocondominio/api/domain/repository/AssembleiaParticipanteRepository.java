package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipante;
import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipanteId;
import br.com.gestaocondominio.api.domain.entity.Assembleia; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssembleiaParticipanteRepository extends JpaRepository<AssembleiaParticipante, AssembleiaParticipanteId> {
   
    List<AssembleiaParticipante> findByAssembleia(Assembleia assembleia);
}