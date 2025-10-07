package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.TipoSolicitacaoManutencao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TipoSolicitacaoManutencaoRepository extends JpaRepository<TipoSolicitacaoManutencao, Integer> {
    Optional<TipoSolicitacaoManutencao> findByTsmDescricao(String descricao);
    List<TipoSolicitacaoManutencao> findByTsmAtiva(boolean ativa);
}