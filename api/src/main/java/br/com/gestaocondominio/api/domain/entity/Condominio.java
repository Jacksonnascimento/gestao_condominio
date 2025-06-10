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

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_CONDOMINIO", schema = "dbo")
public class Condominio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CON_COD")
    private Integer conCod;

    @Column(name = "CON_NOME", nullable = false, length = 100)
    private String conNome;

    @Column(name = "CON_LOGRADOURO", length = 100)
    private String conLogradouro;

    @Column(name = "CON_NUMERO", length = 10)
    private String conNumero;

    @Column(name = "CON_COMPLEMENTO", length = 50)
    private String conComplemento;

    @Column(name = "CON_BAIRRO", length = 50)
    private String conBairro;

    @Column(name = "CON_CIDADE", length = 50)
    private String conCidade;

    @Column(name = "CON_ESTADO", length = 2)
    private String conEstado;

    @Column(name = "CON_CEP", length = 8)
    private String conCep;

    @Column(name = "CON_PAIS", length = 50)
    private String conPais;

    @Column(name = "CON_REFERENCIA", length = 100)
    private String conReferencia;

    @Column(name = "CON_NUMERO_UNIDADES")
    private Integer conNumeroUnidades;

    @Column(name = "CON_TIPOLOGIA", length = 1)
    private Character conTipologia;

    @Column(name = "CON_DT_VENCIMENTO_TAXA")
    private Integer conDtVencimentoTaxa;

    @Column(name = "CON_DT_CADASTRO")
    private LocalDateTime conDtCadastro;

    @Column(name = "CON_DT_ATUALIZACAO")
    private LocalDateTime conDtAtualizacao;

    @ManyToOne
    @JoinColumn(name = "ADM_COD")
    private Administradora administradora;

    @Column(name = "CON_ATIVO") // <-- NOVA COLUNA para soft delete
    private Boolean conAtivo;
}