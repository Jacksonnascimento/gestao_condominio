package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.ReservaAreaComum;
import br.com.gestaocondominio.api.domain.enums.ReservaAreaComumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaAreaComumRepository extends JpaRepository<ReservaAreaComum, Integer> {

    
    List<ReservaAreaComum> findByAreaComum_CondominioIn(List<Condominio> condominios);

    List<ReservaAreaComum> findBySolicitante(Pessoa solicitante);
    
    List<ReservaAreaComum> findByAreaComum(AreaComum areaComum);

    List<ReservaAreaComum> findByUnidadeAndStatusNotIn(
            br.com.gestaocondominio.api.domain.entity.Unidade unidade,
            List<ReservaAreaComumStatus> status
    );

    List<ReservaAreaComum> findByAreaComumAndStatusNotInAndDataHoraFimAfterAndDataHoraInicioBefore(
        AreaComum areaComum,
        List<ReservaAreaComumStatus> status,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim
    );

    List<ReservaAreaComum> findByAreaComumAndStatusNotInAndRacCodNotAndDataHoraFimAfterAndDataHoraInicioBefore(
        AreaComum areaComum,
        List<ReservaAreaComumStatus> status,
        Integer reservaId,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim
    );
}