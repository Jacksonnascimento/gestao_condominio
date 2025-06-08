package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // Import para o BigDecimal
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "GC_UNIDADE", schema = "dbo") 
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UNI_COD")
    private Integer uniCod;

    @Column(name = "UNI_NUMERO", nullable = false, length = 10)
    private String uniNumero;

  
    @Column(name = "UNI_STATUS_OCUPACAO", length = 1)
    private Character uniStatusOcupacao;

  
    @Column(name = "UNI_VALOR_TAXA_CONDOMINIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal uniValorTaxaCondominio;

    @Column(name = "UNI_DT_CADASTRO")
    private LocalDateTime uniDtCadastro;

    @Column(name = "UNI_DT_ATUALIZACAO")
    private LocalDateTime uniDtAtualizacao;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;
}