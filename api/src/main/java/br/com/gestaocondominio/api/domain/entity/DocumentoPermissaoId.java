package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor; 
import lombok.Data;         
import lombok.NoArgsConstructor; 
import java.io.Serializable;
import java.util.Objects; 

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DocumentoPermissaoId implements Serializable {

    @Column(name = "DOC_COD")
    private Integer docCod;

    @Column(name = "PES_COD")
    private Integer pesCod;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentoPermissaoId that = (DocumentoPermissaoId) o;
        return Objects.equals(docCod, that.docCod) &&
               Objects.equals(pesCod, that.pesCod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docCod, pesCod);
    }
}