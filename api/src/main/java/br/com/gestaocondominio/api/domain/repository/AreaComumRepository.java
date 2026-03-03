package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaComumRepository extends JpaRepository<AreaComum, Integer> {

    @EntityGraph(attributePaths = {"condominio", "turnos"})
    List<AreaComum> findByCondominioConCodOrderByNomeAsc(Integer conCod);

    @EntityGraph(attributePaths = {"condominio", "turnos"})
    List<AreaComum> findByCondominioConCodAndAtivaTrueOrderByNomeAsc(Integer conCod);

    @EntityGraph(attributePaths = {"condominio", "turnos"})
    Optional<AreaComum> findById(Integer areCod);
}