package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "GC_AREA_COMUM_TURNO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "turCod")
public class AreaComumTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TUR_COD")
    private Integer turCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARE_COD", nullable = false)
    private AreaComum areaComum;

    @Column(name = "TUR_NOME", nullable = false, length = 50)
    private String nome;

    @Column(name = "TUR_HORA_INICIO", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "TUR_HORA_FIM", nullable = false)
    private LocalTime horaFim;

    @Column(name = "TUR_ATIVO")
    @Builder.Default
    private Boolean ativo = true;
}