package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.CobrancaStatus; 
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_FINANCEIRO_COBRANCA", schema = "dbo")
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