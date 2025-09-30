package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.TipoPeriodoOcupante;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "ocuCod")
@ToString(of = {"ocuCod", "ocuVinculo"})
@Entity
@Table(name = "gc_ocupante") 
public class Ocupante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OCU_COD") 
    private Integer ocuCod;

    @ManyToOne
    @JoinColumn(name = "PES_COD", nullable = false)
    private Pessoa pessoa;

    @ManyToOne
    @JoinColumn(name = "UNI_COD", nullable = false)
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "OCU_VINCULO", nullable = false, length = 50) 
    private OcupanteVinculo ocuVinculo;

    @Column(name = "OCU_DT_INICIO_OCUPACAO") 
    private LocalDate ocuDtInicioOcupacao;

    @Column(name = "OCU_DT_FIM_OCUPACAO") 
    private LocalDate ocuDtFimOcupacao;

    @Column(name = "OCU_PERIODO_USO", length = 100) 
    private String ocuPeriodoUso;

    @Enumerated(EnumType.STRING)
    @Column(name = "OCU_TIPO_PERIODO", length = 50) 
    private TipoPeriodoOcupante ocuTipoPeriodo;

    @Column(name = "OCU_DT_CADASTRO", nullable = false) 
    private LocalDateTime ocuDtCadastro;

    @Column(name = "OCU_DT_ATUALIZACAO") 
    private LocalDateTime ocuDtAtualizacao;
}