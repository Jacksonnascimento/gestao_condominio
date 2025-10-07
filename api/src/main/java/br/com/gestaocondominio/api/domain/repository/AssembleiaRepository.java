package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Assembleia;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssembleiaRepository extends JpaRepository<Assembleia, Integer> {
    List<Assembleia> findByAssAtiva(boolean ativa);
    List<Assembleia> findByCondominioIn(List<Condominio> condominios);
    List<Assembleia> findByCondominio(Condominio condominio);
}