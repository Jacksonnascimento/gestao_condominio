package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "GC_AREA_COMUM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "areCod")
public class AreaComum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ARE_COD")
    private Integer areCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @Column(name = "ARE_NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "ARE_DESCRICAO", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "ARE_TERMOS_USO", columnDefinition = "TEXT")
    private String termosUso;

    @Column(name = "ARE_CAPACIDADE_MAXIMA")
    private Integer capacidadeMaxima;

    @Column(name = "ARE_PERMITE_CONVIDADOS")
    @Builder.Default
    private Boolean permiteConvidados = false;

    @Column(name = "ARE_LIMITE_CONVIDADOS")
    private Integer limiteConvidados;

    @Column(name = "ARE_DIAS_ANTECEDENCIA_MIN")
    @Builder.Default
    private Integer diasAntecedenciaMin = 1;

    @Column(name = "ARE_DIAS_ANTECEDENCIA_MAX")
    @Builder.Default
    private Integer diasAntecedenciaMax = 30;

    @Column(name = "ARE_ATIVA")
    @Builder.Default
    private Boolean ativa = true;

    @Column(name = "ARE_TAXA_VALOR", precision = 10, scale = 2)
    private BigDecimal taxaValor;

    @CreationTimestamp
    @Column(name = "ARE_DT_CADASTRO", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @UpdateTimestamp
    @Column(name = "ARE_DT_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "areaComum", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AreaComumTurno> turnos;
}