package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.ReservaAreaComum;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.ReservaAreaComumStatus; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface ReservaAreaComumRepository extends JpaRepository<ReservaAreaComum, Integer> {
    List<ReservaAreaComum> findByAreaComumAndDataHoraFimAfterAndDataHoraInicioBefore(
        AreaComum areaComum,
        LocalDateTime fim,
        LocalDateTime inicio
    );

    List<ReservaAreaComum> findByAreaComumAndDataHoraFimAfterAndDataHoraInicioBeforeAndRacCodIsNot(
        AreaComum areaComum,
        LocalDateTime fim,
        LocalDateTime inicio,
        Integer idReservaExcluir
    );

    List<ReservaAreaComum> findByAreaComum(AreaComum areaComum);

   
    List<ReservaAreaComum> findByUnidadeAndStatusNotIn(
        Unidade unidade,
        List<ReservaAreaComumStatus> status 
    );
}