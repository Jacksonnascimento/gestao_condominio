package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaComumRepository extends JpaRepository<AreaComum, Integer> {

    List<AreaComum> findByCondominio(Condominio condominio);

    
    List<AreaComum> findByCondominioIn(List<Condominio> condominios);
    
}