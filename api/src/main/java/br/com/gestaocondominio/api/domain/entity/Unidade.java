package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "uniCod")
@ToString(of = {"uniCod", "uniNumero", "bloco", "andar"})
@Entity
@Table(name = "gc_unidade")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UNI_COD")
    private Integer uniCod;

    @Column(name = "UNI_NUMERO", nullable = false, length = 10)
    private String uniNumero;
    
    @Column(name = "UNI_BLOCO", length = 50)
    private String bloco;

    @Column(name = "UNI_ANDAR", length = 50)
    private String andar;

    @Enumerated(EnumType.STRING)
    @Column(name = "UNI_STATUS_OCUPACAO", length = 50)
    private UnidadeStatusOcupacao uniStatusOcupacao;

    @Column(name = "UNI_FRACAO_IDEAL", precision = 5, scale = 4)
    private BigDecimal fracaoIdeal;

    @Column(name = "UNI_AREA_PRIVADA", precision = 10, scale = 2)
    private BigDecimal areaPrivada;
    
    @Lob
    @Column(name = "UNI_OBSERVACAO", columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "UNI_DT_CADASTRO", nullable = false)
    private LocalDateTime uniDtCadastro;

    @Column(name = "UNI_DT_ATUALIZACAO")
    private LocalDateTime uniDtAtualizacao;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @Column(name = "UNI_ATIVA")
    private Boolean uniAtiva;

    @ManyToOne
    @JoinColumn(name = "UTI_COD")
    private UnidadeTipo unidadeTipo;
}