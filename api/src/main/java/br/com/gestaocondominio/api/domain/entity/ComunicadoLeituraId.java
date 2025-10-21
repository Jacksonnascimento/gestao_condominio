package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ComunicadoLeituraId implements Serializable {

    @Column(name = "COM_COD")
    private Integer comunicadoId;

    @Column(name = "PES_COD")
    private Integer pessoaId;
}