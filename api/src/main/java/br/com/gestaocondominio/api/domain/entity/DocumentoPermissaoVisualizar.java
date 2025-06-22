package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(of = "id")
@Entity
@Table(name = "gc_documento_permissao_visualizar")
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