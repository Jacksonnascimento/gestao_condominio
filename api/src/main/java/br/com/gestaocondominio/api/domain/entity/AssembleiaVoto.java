package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.VotoOpcao;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@ToString(of = {"id", "voto"})
@Entity
@Table(name = "gc_assembleia_voto")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "ASV_VOTO", length = 50)
    private VotoOpcao voto;
}