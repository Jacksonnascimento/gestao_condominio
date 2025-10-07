package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.SolicitacaoManutencao;
import br.com.gestaocondominio.api.domain.entity.TipoSolicitacaoManutencao;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.SolicitacaoManutencaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface SolicitacaoManutencaoRepository extends JpaRepository<SolicitacaoManutencao, Integer> {
    List<SolicitacaoManutencao> findByCondominioIn(List<Condominio> condominios);
    List<SolicitacaoManutencao> findBySolicitante(Pessoa solicitante);
    List<SolicitacaoManutencao> findByTipoSolicitacao(TipoSolicitacaoManutencao tipoSolicitacao);
    List<SolicitacaoManutencao> findByUnidadeAndStatusNotIn(Unidade unidade, Collection<SolicitacaoManutencaoStatus> status);
    List<SolicitacaoManutencao> findByCondominio(Condominio condominio);
}