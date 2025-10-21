package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.ComunicadoLeitura;
import br.com.gestaocondominio.api.domain.entity.ComunicadoLeituraId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ComunicadoLeituraRepository extends JpaRepository<ComunicadoLeitura, ComunicadoLeituraId> {

    @Modifying
    @Query("DELETE FROM ComunicadoLeitura cl WHERE cl.id.comunicadoId = :comunicadoId")
    void deleteByComunicadoId(@Param("comunicadoId") Integer comunicadoId);
}