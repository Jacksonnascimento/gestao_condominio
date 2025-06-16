package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.SolicitacaoManutencao;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.TipoSolicitacaoManutencao;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.SolicitacaoManutencaoStatus; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitacaoManutencaoRepository extends JpaRepository<SolicitacaoManutencao, Integer> {
    List<SolicitacaoManutencao> findByCondominio(Condominio condominio);
    List<SolicitacaoManutencao> findByTipoSolicitacao(TipoSolicitacaoManutencao tipoSolicitacao);

   
    List<SolicitacaoManutencao> findByUnidadeAndStatusNotIn(
        Unidade unidade,
        List<SolicitacaoManutencaoStatus> status 
    );
}