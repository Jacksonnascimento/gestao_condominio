package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.SolicitacaoManutencao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SolicitacaoManutencaoRepository extends JpaRepository<SolicitacaoManutencao, Integer> {
    
}