package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Comunicado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface ComunicadoRepository extends JpaRepository<Comunicado, Integer>, JpaSpecificationExecutor<Comunicado> {

    @Override
    @NonNull

    Page<Comunicado> findAll(@NonNull Specification<Comunicado> spec, @NonNull Pageable pageable);
}