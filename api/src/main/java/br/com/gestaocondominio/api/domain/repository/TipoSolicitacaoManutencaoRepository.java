package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.TipoSolicitacaoManutencao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoSolicitacaoManutencaoRepository extends JpaRepository<TipoSolicitacaoManutencao, Integer> {
    Optional<TipoSolicitacaoManutencao> findByTsmDescricao(String tsmDescricao);
    List<TipoSolicitacaoManutencao> findByTsmAtiva(Boolean tsmAtiva);
}