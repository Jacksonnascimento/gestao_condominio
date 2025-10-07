package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.ReservaAreaComum;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.ReservaAreaComumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservaAreaComumRepository extends JpaRepository<ReservaAreaComum, Integer> {
    List<ReservaAreaComum> findByAreaComum(AreaComum areaComum);
    List<ReservaAreaComum> findByUnidadeAndStatusNotIn(Unidade unidade, Collection<ReservaAreaComumStatus> status);
    List<ReservaAreaComum> findByAreaComum_CondominioIn(List<Condominio> condominios);
    List<ReservaAreaComum> findBySolicitante(Pessoa solicitante);
    List<ReservaAreaComum> findByAreaComumAndStatusNotInAndDataHoraFimAfterAndDataHoraInicioBefore(AreaComum areaComum, Collection<ReservaAreaComumStatus> status, LocalDateTime dataHoraFim, LocalDateTime dataHoraInicio);
}