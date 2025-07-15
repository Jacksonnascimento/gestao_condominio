package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.DespesaStatusPagamento; 
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "desCod")
@ToString(of = {"desCod", "desDescricao", "desValor", "desStatusPagamento"})
@Entity
@Table(name = "gc_financeiro_despesa")
public class FinanceiroDespesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DES_COD")
    private Integer desCod;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @Column(name = "DES_DESCRICAO", nullable = false, length = 255)
    private String desDescricao;

    @Column(name = "DES_VALOR", nullable = false, precision = 10, scale = 2)
    private BigDecimal desValor;

    @Column(name = "DES_DATA_VENCIMENTO", nullable = false)
    private LocalDate desDataVencimento;

    @Column(name = "DES_DATA_PAGAMENTO")
    private LocalDate desDataPagamento;

    @Enumerated(EnumType.STRING) 
    @Column(name = "DES_STATUS_PAGAMENTO", nullable = false, length = 50)
    private DespesaStatusPagamento desStatusPagamento;

    @ManyToOne
    @JoinColumn(name = "DCA_COD", nullable = false)
    private DespesaCategoria categoria;
}