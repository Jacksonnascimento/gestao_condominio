package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.TaxaCondominioValor;
import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import br.com.gestaocondominio.api.domain.entity.UnidadeTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaxaCondominioValorRepository extends JpaRepository<TaxaCondominioValor, Integer> {

    Optional<TaxaCondominioValor> findByUnidadeTipoAndTipoCobranca(UnidadeTipo unidadeTipo, TipoCobranca tipoCobranca);

    Optional<TaxaCondominioValor> findByUnidadeTipoUtiCodAndTipoCobrancaTicCod(Integer utiCod, Integer ticCod);

}