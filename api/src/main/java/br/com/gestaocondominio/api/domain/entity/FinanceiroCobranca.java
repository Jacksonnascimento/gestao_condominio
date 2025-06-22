package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.CobrancaStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "ficCod")
@ToString(of = {"ficCod", "ficStatusPagamento", "ficValorTaxa"})
@Entity
@Table(name = "gc_financeiro_cobranca")
public class FinanceiroCobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FIC_COD")
    private Integer ficCod;

    @ManyToOne
    @JoinColumn(name = "UNI_COD", nullable = false)
    private Unidade unidade;

    @Column(name = "FIC_VALOR_TAXA", nullable = false, precision = 10, scale = 2)
    private BigDecimal ficValorTaxa;

    @Column(name = "FIC_DT_VENCIMENTO", nullable = false)
    private LocalDate ficDtVencimento;

    @Column(name = "FIC_DT_PAGAMENTO")
    private LocalDate ficDtPagamento;

    @ManyToOne
    @JoinColumn(name = "TIC_COD", nullable = false)
    private TipoCobranca tipoCobranca;

    @Column(name = "FIC_VALOR_PAGO", precision = 10, scale = 2)
    private BigDecimal ficValorPago;

    @Column(name = "FIC_DT_CADASTRO")
    private LocalDateTime ficDtCadastro;

    @Column(name = "FIC_DT_ATUALIZACAO")
    private LocalDateTime ficDtAtualizacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "FIC_STATUS_PAGAMENTO", nullable = false, length = 50)
    private CobrancaStatus ficStatusPagamento;
}