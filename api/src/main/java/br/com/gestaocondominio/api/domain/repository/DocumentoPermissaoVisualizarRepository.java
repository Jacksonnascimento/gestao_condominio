package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Documento;
import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoId;
import br.com.gestaocondominio.api.domain.entity.DocumentoPermissaoVisualizar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoPermissaoVisualizarRepository extends JpaRepository<DocumentoPermissaoVisualizar, DocumentoPermissaoId> {
    
    List<DocumentoPermissaoVisualizar> findByDocumento(Documento documento);

    boolean existsById_DocCodAndId_PesCod(Integer docCod, Integer pesCod);
}