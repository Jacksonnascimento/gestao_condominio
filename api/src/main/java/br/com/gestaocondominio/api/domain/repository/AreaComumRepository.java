package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /** Áreas de vários condomínios de uma vez (API v1: quem gerencia ou mora em mais de um condomínio). */
    @EntityGraph(attributePaths = {"condominio", "turnos"})
    List<AreaComum> findByCondominioConCodInOrderByNomeAsc(Collection<Integer> conCods);

    /** Todas as áreas do cliente (API v1: administrador geral sem condomínio escolhido). */
    @EntityGraph(attributePaths = {"condominio", "turnos"})
    List<AreaComum> findAllByOrderByNomeAsc();
}
