package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoCobrancaRepository extends JpaRepository<TipoCobranca, Integer> {
    Optional<TipoCobranca> findByTicDescricao(String ticDescricao);

    
    List<TipoCobranca> findByTicAtiva(Boolean ticAtiva); 
}