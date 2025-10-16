package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UnidadeRepository extends JpaRepository<Unidade, Integer> {
    Optional<Unidade> findByUniNumeroAndCondominio(String uniNumero, Condominio condominio);

    @Query("SELECT u FROM Unidade u JOIN FETCH u.condominio WHERE u.uniAtiva = :ativa")
    List<Unidade> findByUniAtivaWithCondominio(boolean ativa);
    
    @Query("SELECT u FROM Unidade u JOIN FETCH u.condominio")
    List<Unidade> findAllWithCondominio();
    
    @Query("SELECT u from Unidade u WHERE u.condominio IN :condominios")
    List<Unidade> findByCondominioInWithCondominio(List<Condominio> condominios);

    List<Unidade> findByCondominioConCod(Integer conCod);

    List<Unidade> findByCondominio(Condominio condominio);
    
    List<Unidade> findByCondominioConCodAndUniAtivaTrue(Integer conCod);
   

    @Query("SELECT u FROM Unidade u JOIN FETCH u.condominio WHERE u.uniCod = :id")
    Optional<Unidade> findByIdWithCondominio(@Param("id") Integer id);
}