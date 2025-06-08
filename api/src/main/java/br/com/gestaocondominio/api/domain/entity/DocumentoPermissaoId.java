package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DocumentoPermissaoId implements Serializable {

    @Column(name = "DOC_COD")
    private Integer docCod;

    @Column(name = "PES_COD")
    private Integer pesCod;
}