package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.SolicitacaoManutencaoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "smaCod")
@ToString(of = {"smaCod", "status", "localDescricao"})
@Entity
@Table(name = "gc_solicitacao_manutencao")
public class SolicitacaoManutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SMA_COD")
    private Integer smaCod;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @ManyToOne
    @JoinColumn(name = "UNI_COD")
    private Unidade unidade;

    @ManyToOne
    @JoinColumn(name = "PES_COD_SOLICITANTE", nullable = false)
    private Pessoa solicitante;

    @ManyToOne
    @JoinColumn(name = "PES_COD_RESPONSAVEL")
    private Pessoa responsavel;

    @Column(name = "SMA_LOCAL_DESCRICAO", length = 255)
    private String localDescricao;

    @ManyToOne
    @JoinColumn(name = "TSM_COD", nullable = false)
    private TipoSolicitacaoManutencao tipoSolicitacao;

    @Lob
    @Column(name = "SMA_DESCRICAO_PROBLEMA", nullable = false, columnDefinition = "TEXT")
    private String descricaoProblema;

    @Enumerated(EnumType.STRING)
    @Column(name = "SMA_STATUS", nullable = false, length = 50)
    private SolicitacaoManutencaoStatus status;

    @Column(name = "SMA_DT_ABERTURA")
    private LocalDateTime dtAbertura;

    @Column(name = "SMA_DT_CONCLUSAO")
    private LocalDateTime dtConclusao;

    @Column(name = "SMA_DT_ATUALIZACAO")
    private LocalDateTime dtAtualizacao;
}