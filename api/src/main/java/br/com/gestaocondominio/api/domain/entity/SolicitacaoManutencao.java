package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_SOLICITACAO_MANUTENCAO", schema = "dbo")
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
    @JoinColumn(name = "TSM_COD", nullable = false) // <-- FK para a nova tabela
    private TipoSolicitacaoManutencao tipoSolicitacao; // <-- NOVO CAMPO

    @Lob
    @Column(name = "SMA_DESCRICAO_PROBLEMA", nullable = false)
    private String descricaoProblema;

    @Column(name = "SMA_STATUS", nullable = false, length = 30)
    private String status;

    @Column(name = "SMA_DT_ABERTURA")
    private LocalDateTime dtAbertura;

    @Column(name = "SMA_DT_CONCLUSAO")
    private LocalDateTime dtConclusao;

    @Column(name = "SMA_DT_ATUALIZACAO")
    private LocalDateTime dtAtualizacao;
}