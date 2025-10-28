package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "GC_OCORRENCIA_COMENTARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "occCod")
public class OcorrenciaComentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OCC_COD")
    private Integer occCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OCO_COD", nullable = false)
    private Ocorrencia ocorrencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_COMENTARIO", nullable = false)
    private Pessoa pessoaComentario;

    @Column(name = "OCC_COMENTARIO", nullable = false, columnDefinition = "TEXT")
    private String comentario;

    @CreationTimestamp
    @Column(name = "OCC_DT_COMENTARIO", nullable = false, updatable = false)
    private LocalDateTime dataComentario;
}