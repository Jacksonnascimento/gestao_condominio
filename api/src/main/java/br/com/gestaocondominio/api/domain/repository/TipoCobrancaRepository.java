package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TipoCobrancaRepository extends JpaRepository<TipoCobranca, Integer> {
    Optional<TipoCobranca> findByCondominioAndTicIsTaxaPrincipal(Condominio condominio, boolean isTaxaPrincipal);
    Optional<TipoCobranca> findByTicDescricaoAndCondominio(String descricao, Condominio condominio);
    List<TipoCobranca> findByTicAtiva(boolean ativa);
    List<TipoCobranca> findByCondominio(Condominio condominio);
    List<TipoCobranca> findByCondominioAndTicAtiva(Condominio condominio, boolean ativa);
}