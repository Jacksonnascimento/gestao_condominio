package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.CondominioTipologia;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "conCod")
@ToString(of = {"conCod", "conNome"})
@Entity
@Table(name = "gc_condominio")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "CON_TIPOLOGIA", nullable = false, length = 50)
    private CondominioTipologia conTipologia;

    @Column(name = "CON_DT_VENCIMENTO_TAXA")
    private Integer conDtVencimentoTaxa;

    @Column(name = "CON_DT_CADASTRO")
    private LocalDateTime conDtCadastro;

    @Column(name = "CON_DT_ATUALIZACAO")
    private LocalDateTime conDtAtualizacao;

    @Column(name = "CON_ATIVO")
    private Boolean conAtivo;

    @Column(name = "CON_GERACAO_AUTO_ATIVA")
    private Boolean conGeracaoAutoAtiva;

    @Column(name = "CON_DIA_GERACAO_COBRANCA")
    private Integer conDiaGeracaoCobranca;
}