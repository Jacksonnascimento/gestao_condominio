package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Assembleia;
import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipante;
import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipanteId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssembleiaParticipanteRepository extends JpaRepository<AssembleiaParticipante, AssembleiaParticipanteId> {
    List<AssembleiaParticipante> findByAssembleia(Assembleia assembleia);
}