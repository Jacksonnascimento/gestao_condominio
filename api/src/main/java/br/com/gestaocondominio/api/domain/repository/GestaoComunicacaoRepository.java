package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.GestaoComunicacao;
import br.com.gestaocondominio.api.domain.enums.ComunicadoDestino;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GestaoComunicacaoRepository extends JpaRepository<GestaoComunicacao, Integer> {
    List<GestaoComunicacao> findByCondominio(Condominio condominio);
    List<GestaoComunicacao> findByCondominioAndComDesTodos(Condominio condominio, ComunicadoDestino destino);
}