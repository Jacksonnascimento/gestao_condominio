package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Ocorrencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Integer>, JpaSpecificationExecutor<Ocorrencia> {

    
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"condominio", "unidade", "pessoaRegistro", "pessoaFinalizou", "comentarios", "anexos"})
    Optional<Ocorrencia> findById(@NonNull Integer id);
}