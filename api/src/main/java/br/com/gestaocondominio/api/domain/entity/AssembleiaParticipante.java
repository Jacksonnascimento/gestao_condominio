package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_ASSEMBLEIA_PARTICIPANTE", schema = "dbo")
public class AssembleiaParticipante {

    @EmbeddedId 
    private AssembleiaParticipanteId id;

    @ManyToOne
    @MapsId("assCod") 
    @JoinColumn(name = "ASS_COD")
    private Assembleia assembleia;

    @ManyToOne
    @MapsId("pesCod") 
    @JoinColumn(name = "PES_COD")
    private Pessoa pessoa;

    @Column(name = "ASP_PARTICIPACAO")
    private Boolean participacao;
}