package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@EqualsAndHashCode(of = "cmeCod")
@ToString(of = {"cmeCod", "statusLeitura", "statusEnvio"})
@Entity
@Table(name = "GC_COMUNICADO_ENTREGA", schema = "dbo",
    uniqueConstraints = @UniqueConstraint(columnNames = {"COM_COD", "PES_COD"})
)
public class ComunicadoEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CME_COD")
    private Integer cmeCod;

    @ManyToOne
    @JoinColumn(name = "COM_COD", nullable = false)
    private GestaoComunicacao comunicado;

    @ManyToOne
    @JoinColumn(name = "PES_COD", nullable = false)
    private Pessoa destinatario;

    @Column(name = "CME_STATUS_LEITURA", nullable = false)
    private Boolean statusLeitura;

    @Column(name = "CME_DT_LEITURA")
    private LocalDateTime dtLeitura;

    @Column(name = "CME_STATUS_ENVIO", nullable = false, length = 20)
    private String statusEnvio;

    @Column(name = "CME_DT_ENVIO_INDIVIDUAL")
    private LocalDateTime dtEnvioIndividual;
}