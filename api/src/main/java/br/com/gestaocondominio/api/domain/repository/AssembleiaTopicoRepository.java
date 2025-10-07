package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Assembleia;
import br.com.gestaocondominio.api.domain.entity.AssembleiaTopico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssembleiaTopicoRepository extends JpaRepository<AssembleiaTopico, Integer> {
    List<AssembleiaTopico> findByAssembleia(Assembleia assembleia);
}