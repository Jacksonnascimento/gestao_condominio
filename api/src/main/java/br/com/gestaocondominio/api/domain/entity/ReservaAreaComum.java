package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.ReservaAreaComumStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "racCod")
@ToString(of = {"racCod", "status", "dataHoraInicio"})
@Entity
@Table(name = "gc_reserva_area_comum")
public class ReservaAreaComum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RAC_COD")
    private Integer racCod;

    @ManyToOne
    @JoinColumn(name = "ARC_COD", nullable = false)
    private AreaComum areaComum;

    @ManyToOne
    @JoinColumn(name = "UNI_COD", nullable = false)
    private Unidade unidade;

    @ManyToOne
    @JoinColumn(name = "PES_COD_SOLICITANTE", nullable = false)
    private Pessoa solicitante;

    @Column(name = "RAC_DATA_HORA_INICIO", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "RAC_DATA_HORA_FIM", nullable = false)
    private LocalDateTime dataHoraFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "RAC_STATUS", nullable = false, length = 50)
    private ReservaAreaComumStatus status;

    @Column(name = "RAC_OBSERVACOES", length = 500)
    private String observacoes;

    @Column(name = "RAC_DT_SOLICITACAO")
    private LocalDateTime dtSolicitacao;

    @Column(name = "RAC_DT_ATUALIZACAO")
    private LocalDateTime dtAtualizacao;
}