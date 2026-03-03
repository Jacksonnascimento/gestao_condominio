package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Reserva;
import br.com.gestaocondominio.api.domain.enums.ReservaStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    // Carrega antecipadamente todos os objetos aninhados necessários para a tela
    @EntityGraph(attributePaths = {"areaComum", "areaComum.condominio", "turno", "unidade", "morador", "convidados"})
    List<Reserva> findByAreaComumCondominioConCodOrderByDataDesc(Integer conCod);

    @EntityGraph(attributePaths = {"areaComum", "areaComum.condominio", "turno", "unidade", "morador", "convidados"})
    List<Reserva> findByMoradorPesCodOrderByDataDesc(Integer pesCod);

    // Sobrescrevemos o findById padrão para garantir que a aprovação/cancelamento retorne o card com os dados prontos
    @EntityGraph(attributePaths = {"areaComum", "areaComum.condominio", "turno", "unidade", "morador", "convidados"})
    Optional<Reserva> findById(Integer resCod);

    List<Reserva> findByAreaComumAreCodAndDataAndStatusNot(Integer areCod, LocalDate data, ReservaStatus status);

    List<Reserva> findByAreaComumAreCodAndTurnoTurCodAndDataAndStatusNot(Integer areCod, Integer turCod, LocalDate data, ReservaStatus status);

    long countByUnidadeUniCodAndDataGreaterThanEqualAndStatusNot(Integer uniCod, LocalDate data, ReservaStatus status);
}