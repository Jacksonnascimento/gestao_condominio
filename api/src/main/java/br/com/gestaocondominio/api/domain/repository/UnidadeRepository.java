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

    @Query("SELECT u FROM Unidade u JOIN FETCH u.condominio")
    List<Unidade> findAllWithCondominio();

    @Query("SELECT u FROM Unidade u JOIN FETCH u.condominio WHERE u.condominio IN :condominios")
    List<Unidade> findByCondominioInWithCondominio(@Param("condominios") List<Condominio> condominios);

    Optional<Unidade> findByUniNumeroAndCondominio(String uniNumero, Condominio condominio);

    List<Unidade> findByCondominio(Condominio condominio);

    @Query("SELECT u FROM Unidade u JOIN FETCH u.condominio WHERE u.uniAtiva = :ativa")
    List<Unidade> findByUniAtivaWithCondominio(@Param("ativa") boolean ativa);

    List<Unidade> findByCondominioConCodAndUniAtivaTrue(Integer conCod);
    
}