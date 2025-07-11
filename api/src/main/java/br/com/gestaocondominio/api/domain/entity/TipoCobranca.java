package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "ticCod")
@ToString(of = {"ticCod", "ticDescricao", "ticAtiva"})
@Entity
@Table(name = "gc_tipo_cobranca")
public class TipoCobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TIC_COD")
    private Integer ticCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @Column(name = "TIC_DESCRICAO", nullable = false, length = 100)
    private String ticDescricao;

    @Column(name = "TIC_VALOR", nullable = false, precision = 10, scale = 2)
    private BigDecimal ticValor;

    @Column(name = "TIC_DT_CADASTRO")
    private LocalDateTime ticDtCadastro;

    @Column(name = "TIC_DT_ATUALIZACAO")
    private LocalDateTime ticDtAtualizacao;

    @Column(name = "TIC_ATIVA")
    private Boolean ticAtiva;
}