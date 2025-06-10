package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Documento;
import br.com.gestaocondominio.api.domain.entity.Assembleia; 
import br.com.gestaocondominio.api.domain.entity.Condominio; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Integer> {

    List<Documento> findByCondominio(Condominio condominio); 
    List<Documento> findByAssembleia(Assembleia assembleia);
}