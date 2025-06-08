package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_DOCUMENTO_PERMISSAO_VISUALIZAR", schema = "dbo") // Nome corrigido
public class DocumentoPermissaoVisualizar {

    @EmbeddedId
    private DocumentoPermissaoId id;

    @ManyToOne
    @JoinColumn(name = "DOC_COD", insertable = false, updatable = false)
    private Documento documento;

    @ManyToOne
    @JoinColumn(name = "PES_COD", insertable = false, updatable = false)
    private Pessoa pessoa;
}