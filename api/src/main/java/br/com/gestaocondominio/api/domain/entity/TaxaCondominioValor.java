package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "tcvCod")
@ToString(of = {"tcvCod", "tcvValor"})
@Entity
@Table(name = "gc_taxa_condominio_valor")
public class TaxaCondominioValor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TCV_COD")
    private Integer tcvCod;

    @ManyToOne
    @JoinColumn(name = "UTI_COD", nullable = false)
    private UnidadeTipo unidadeTipo;

    @ManyToOne
    @JoinColumn(name = "TIC_COD", nullable = false)
    private TipoCobranca tipoCobranca;

    @Column(name = "TCV_VALOR", nullable = false, precision = 10, scale = 2)
    private BigDecimal tcvValor;
}