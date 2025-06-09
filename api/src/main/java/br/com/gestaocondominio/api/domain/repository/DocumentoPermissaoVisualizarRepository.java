package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoVisualizar;
import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DocumentoPermissaoVisualizarRepository extends JpaRepository<DocumentoPermissaoVisualizar, DocumentoPermissaoId> {
   
}