package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.OcorrenciaComentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//import java.util.List;

@Repository
public interface OcorrenciaComentarioRepository extends JpaRepository<OcorrenciaComentario, Integer> {

    // para buscar comentários sem carregar a Ocorrência inteira:
    // List<OcorrenciaComentario> findByOcorrenciaOcoCodOrderByDataComentarioDesc(Integer ocorrenciaId);
}