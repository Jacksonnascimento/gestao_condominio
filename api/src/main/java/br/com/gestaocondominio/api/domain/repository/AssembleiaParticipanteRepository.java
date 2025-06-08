package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipante;
import br.com.gestaocondominio.api.domain.entity.AssembleiaParticipanteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssembleiaParticipanteRepository extends JpaRepository<AssembleiaParticipante, AssembleiaParticipanteId> {
}