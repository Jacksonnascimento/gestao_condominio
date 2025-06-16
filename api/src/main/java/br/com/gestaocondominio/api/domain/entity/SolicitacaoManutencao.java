package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.SolicitacaoManutencaoStatus; 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;   
import jakarta.persistence.Enumerated; 
import jakarta.persistence.Lob;
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
    @JoinColumn(name = "TSM_COD", nullable = false)
    private TipoSolicitacaoManutencao tipoSolicitacao;

    @Lob
    @Column(name = "SMA_DESCRICAO_PROBLEMA", nullable = false)
    private String descricaoProblema;

    @Enumerated(EnumType.STRING) // Mapeia o ENUM para String no DB
    @Column(name = "SMA_STATUS", nullable = false, length = 50) // <-- Tipo alterado para String no DB
    private SolicitacaoManutencaoStatus status; // <-- Tipo alterado para SolicitacaoManutencaoStatus

    @Column(name = "SMA_DT_ABERTURA")
    private LocalDateTime dtAbertura;

    @Column(name = "SMA_DT_CONCLUSAO")
    private LocalDateTime dtConclusao;

    @Column(name = "SMA_DT_ATUALIZACAO")
    private LocalDateTime dtAtualizacao;
}