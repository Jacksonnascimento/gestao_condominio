package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AreaComumRepository extends JpaRepository<AreaComum, Integer> {
    List<AreaComum> findByCondominioIn(List<Condominio> condominios);
    List<AreaComum> findByCondominio(Condominio condominio);
}