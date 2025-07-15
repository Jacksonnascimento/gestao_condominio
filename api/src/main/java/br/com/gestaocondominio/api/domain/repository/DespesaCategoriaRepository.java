// src/main/java/br/com/gestaocondominio/api/domain/repository/DespesaCategoriaRepository.java
package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio; 
import br.com.gestaocondominio.api.domain.entity.DespesaCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DespesaCategoriaRepository extends JpaRepository<DespesaCategoria, Integer> {
    Optional<DespesaCategoria> findByDcaDescricaoAndCondominio(String dcaDescricao, Condominio condominio); 
    List<DespesaCategoria> findByDcaAtiva(Boolean dcaAtiva);
    List<DespesaCategoria> findByCondominioAndDcaAtiva(Condominio condominio, Boolean dcaAtiva); 
    List<DespesaCategoria> findByCondominioIn(List<Condominio> condominios); 
}