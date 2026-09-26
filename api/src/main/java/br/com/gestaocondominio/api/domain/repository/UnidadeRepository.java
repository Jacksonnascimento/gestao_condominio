package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.UnidadeTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UnidadeRepository extends JpaRepository<Unidade, Integer>, JpaSpecificationExecutor<Unidade> {
    Optional<Unidade> findByCondominioAndUniNumeroAndBlocoAndUnidadeTipo(Condominio condominio, String uniNumero, String bloco, UnidadeTipo unidadeTipo);


    List<Unidade> findByCondominioConCod(Integer conCod);

    List<Unidade> findByCondominio(Condominio condominio);
    
    
    List<Unidade> findByCondominioConCodAndUniAtivaTrue(Integer conCod);

    
    @Query("SELECT u FROM Unidade u JOIN FETCH u.condominio WHERE u.condominio.conCod = :conCod AND u.uniAtiva = true")
    List<Unidade> findAtivasByCondominioConCodWithCondominio(@Param("conCod") Integer conCod);

    @Query("SELECT u FROM Unidade u JOIN FETCH u.condominio WHERE u.uniCod = :id")
    Optional<Unidade> findByIdWithCondominio(@Param("id") Integer id);
}