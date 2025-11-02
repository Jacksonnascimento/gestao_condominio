package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OcupanteRepository extends JpaRepository<Ocupante, Integer>, JpaSpecificationExecutor<Ocupante> {

    @Query("SELECT o FROM Ocupante o JOIN FETCH o.pessoa JOIN FETCH o.unidade u JOIN FETCH u.condominio")
    List<Ocupante> findAllWithDetails();

    List<Ocupante> findByPessoa(Pessoa pessoa);

    List<Ocupante> findByUnidade(Unidade unidade);

    Optional<Ocupante> findByPessoaAndUnidade(Pessoa pessoa, Unidade unidade);

    List<Ocupante> findByUnidadeIn(List<Unidade> unidades);

    @Query("SELECT o FROM Ocupante o JOIN o.pessoa p JOIN o.unidade u " +
           "WHERE u.condominio.conCod = :condominioId " +
           "AND p.pesEmail IS NOT NULL " +
           "AND NOT EXISTS (SELECT uc FROM UsuarioCondominio uc " +
           "                WHERE uc.pesCod = p.pesCod " +
           "                AND uc.conCod = :condominioId " +
           "                AND uc.uscPapel = 'MORADOR')")
    List<Ocupante> findOcupantesSemLoginMoradorByCondominio(@Param("condominioId") Integer condominioId);
}