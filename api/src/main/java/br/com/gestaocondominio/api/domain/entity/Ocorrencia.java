package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "GC_OCORRENCIA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "ocoCod")
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OCO_COD")
    private Integer ocoCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UNI_COD", nullable = false)
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_REGISTRO", nullable = false)
    private Pessoa pessoaRegistro;

    @Column(name = "OCO_TITULO", nullable = false, length = 150)
    private String titulo;

    @Column(name = "OCO_DESCRICAO", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "OCO_TIPO", nullable = false, length = 50)
    private OcorrenciaTipo tipo;

    @Builder.Default // <-- CORREÇÃO APLICADA AQUI
    @Enumerated(EnumType.STRING)
    @Column(name = "OCO_STATUS", nullable = false, length = 20)
    private OcorrenciaStatus status = OcorrenciaStatus.ABERTA; // Default status

    @CreationTimestamp
    @Column(name = "OCO_DT_REGISTRO", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    @UpdateTimestamp
    @Column(name = "OCO_DT_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @Column(name = "OCO_PARECER_FINAL", columnDefinition = "TEXT")
    private String parecerFinal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_FINALIZOU")
    private Pessoa pessoaFinalizou;

    @Column(name = "OCO_DT_FINALIZACAO")
    private LocalDateTime dataFinalizacao;

   
    @OneToMany(mappedBy = "ocorrencia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dataComentario DESC")
    private List<OcorrenciaComentario> comentarios;

    
    @OneToMany(mappedBy = "ocorrencia", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dataAnexo DESC")
    private List<OcorrenciaAnexo> anexos;
}