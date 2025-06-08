package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_ASSEMBLEIA_VOTO", schema = "dbo")
public class AssembleiaVoto {

    @EmbeddedId
    private AssembleiaVotoId id;

    @ManyToOne
    @MapsId("aspCod")
    @JoinColumn(name = "ASP_COD")
    private AssembleiaTopico topico;

    @ManyToOne
    @MapsId("pesCod")
    @JoinColumn(name = "PES_COD")
    private Pessoa pessoa;

    @Column(name = "ASV_VOTO", length = 1)
    private Character voto;
}