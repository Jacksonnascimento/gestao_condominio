package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_RESERVA_AREA_COMUM", schema = "dbo")
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

    @Column(name = "RAC_STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "RAC_OBSERVACOES", length = 500)
    private String observacoes;

    @Column(name = "RAC_DT_SOLICITACAO")
    private LocalDateTime dtSolicitacao;

    @Column(name = "RAC_DT_ATUALIZACAO")
    private LocalDateTime dtAtualizacao;
}