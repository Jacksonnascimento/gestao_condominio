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
public class UsuarioCondominioId implements Serializable {

    @Column(name = "PES_COD")
    private Integer pesCod;

    @Column(name = "CON_COD")
    private Integer conCod;

    @Column(name = "USC_PAPEL")
    private Character uscPapel;
}