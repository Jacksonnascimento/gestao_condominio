package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoCobrancaRepository extends JpaRepository<TipoCobranca, Integer> {
    Optional<TipoCobranca> findByTicDescricaoAndCondominio(String ticDescricao, Condominio condominio);

    List<TipoCobranca> findByTicAtiva(Boolean ticAtiva);
    
    List<TipoCobranca> findByCondominioAndTicAtiva(Condominio condominio, Boolean ticAtiva);

    List<TipoCobranca> findByCondominio(Condominio condominio);

    Optional<TipoCobranca> findByCondominioAndTicIsTaxaPrincipal(Condominio condominio, Boolean ticIsTaxaPrincipal);

    List<TipoCobranca> findByCondominioConCodAndTicGeracaoAutomaticaIsTrueAndTicAtivaIsTrue(Integer conCod);
}