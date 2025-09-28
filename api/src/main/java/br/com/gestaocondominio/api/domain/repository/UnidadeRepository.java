package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadeRepository extends JpaRepository<Unidade, Integer> {

    @Query("SELECT u FROM Unidade u LEFT JOIN FETCH u.unidadeTipo WHERE u.condominio IN :condominios")
    List<Unidade> findByCondominioInFetchTipo(@Param("condominios") List<Condominio> condominios);

    Optional<Unidade> findByUniNumeroAndCondominio(String uniNumero, Condominio condominio);

    List<Unidade> findByCondominio(Condominio condominio);

    @Query("SELECT u FROM Unidade u LEFT JOIN FETCH u.unidadeTipo WHERE u.uniAtiva = :uniAtiva")
    List<Unidade> findByUniAtivaFetchTipo(@Param("uniAtiva") Boolean uniAtiva);

    List<Unidade> findByCondominioConCodAndUniAtivaTrue(Integer conCod);
    
    @Query("SELECT u FROM Unidade u LEFT JOIN FETCH u.unidadeTipo")
    List<Unidade> findAllFetchTipo();
}