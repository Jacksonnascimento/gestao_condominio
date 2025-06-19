package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
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