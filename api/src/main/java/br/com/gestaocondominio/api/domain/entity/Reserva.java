package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.ReservaStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "GC_RESERVA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "resCod")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RES_COD")
    private Integer resCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARE_COD", nullable = false)
    private AreaComum areaComum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TUR_COD")
    private AreaComumTurno turno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UNI_COD", nullable = false)
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_MORADOR", nullable = false)
    private Pessoa morador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_APROVADOR")
    private Pessoa aprovador;

    @Column(name = "RES_DATA", nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(name = "RES_STATUS", nullable = false, length = 30)
    @Builder.Default
    private ReservaStatus status = ReservaStatus.PENDENTE_APROVACAO;

    @Column(name = "RES_TERMOS_ACEITOS", nullable = false)
    @Builder.Default
    private Boolean termosAceitos = false;

    @Column(name = "RES_MOTIVO_REJEICAO", columnDefinition = "TEXT")
    private String motivoRejeicao;

    @CreationTimestamp
    @Column(name = "RES_DT_REGISTRO", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    @UpdateTimestamp
    @Column(name = "RES_DT_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReservaConvidado> convidados;
}