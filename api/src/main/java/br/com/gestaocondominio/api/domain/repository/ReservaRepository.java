package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Reserva;
import br.com.gestaocondominio.api.domain.enums.ReservaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer>, JpaSpecificationExecutor<Reserva> {


    // Sobrescrevemos o findById padrão para garantir que a aprovação/cancelamento retorne o card com os dados prontos
    @EntityGraph(attributePaths = {"areaComum", "areaComum.condominio", "turno", "unidade", "morador", "convidados"})
    Optional<Reserva> findById(Integer resCod);

    /**
     * Listagem paginada da API v1. Os convidados ficam de fora do grafo de propósito: coleção junto com paginação
     * faria o Hibernate paginar em memória. Eles são carregados depois, só para as reservas da página.
     */
    @EntityGraph(attributePaths = {"areaComum", "areaComum.condominio", "turno", "unidade", "morador"})
    Page<Reserva> findAll(Specification<Reserva> spec, Pageable pageable);

    /** Carrega, numa consulta só, os convidados das reservas de uma página da listagem acima. */
    @EntityGraph(attributePaths = {"convidados"})
    List<Reserva> findByResCodIn(Collection<Integer> resCods);

    List<Reserva> findByAreaComumAreCodAndDataAndStatusNot(Integer areCod, LocalDate data, ReservaStatus status);

    List<Reserva> findByAreaComumAreCodAndTurnoTurCodAndDataAndStatusNot(Integer areCod, Integer turCod, LocalDate data, ReservaStatus status);

    long countByUnidadeUniCodAndDataGreaterThanEqualAndStatusNot(Integer uniCod, LocalDate data, ReservaStatus status);

    /** Se a área já foi reservada alguma vez (nesse caso ela não pode ser excluída, só inativada). */
    boolean existsByAreaComumAreCod(Integer areCod);

    /** Se o turno já foi reservado alguma vez (nesse caso ele não pode sair da área, só ser desativado). */
    boolean existsByTurnoTurCod(Integer turCod);
}
