package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.GestaoComunicacao;
import br.com.gestaocondominio.api.domain.entity.Condominio; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GestaoComunicacaoRepository extends JpaRepository<GestaoComunicacao, Integer> {
    
    List<GestaoComunicacao> findByCondominio(Condominio condominio); 
}