package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "uniCod")
@ToString(of = {"uniCod", "uniNumero"})
@Entity
@Table(name = "gc_unidade")
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UNI_COD")
    private Integer uniCod;

    @Column(name = "UNI_NUMERO", nullable = false, length = 10)
    private String uniNumero;

    @Enumerated(EnumType.STRING)
    @Column(name = "UNI_STATUS_OCUPACAO", length = 50)
    private UnidadeStatusOcupacao uniStatusOcupacao;

    @Column(name = "UNI_VALOR_TAXA_CONDOMINIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal uniValorTaxaCondominio;

    @Column(name = "UNI_DT_CADASTRO", nullable = false)
    private LocalDateTime uniDtCadastro;

    @Column(name = "UNI_DT_ATUALIZACAO")
    private LocalDateTime uniDtAtualizacao;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @Column(name = "UNI_ATIVA")
    private Boolean uniAtiva;
}