package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.OcorrenciaAnexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//import java.util.List;
import java.util.Optional;

@Repository
public interface OcorrenciaAnexoRepository extends JpaRepository<OcorrenciaAnexo, Integer> {

    
    Optional<OcorrenciaAnexo> findByOcorrenciaOcoCodAndOcaCod(Integer ocorrenciaId, Integer anexoId);

    // Se precisarmos buscar anexos sem carregar a Ocorrência inteira:
    // List<OcorrenciaAnexo> findByOcorrenciaOcoCodOrderByDataAnexoDesc(Integer ocorrenciaId);
}