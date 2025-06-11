package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_TIPO_SOLICITACAO_MANUTENCAO", schema = "dbo")
public class TipoSolicitacaoManutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TSM_COD")
    private Integer tsmCod;

    @Column(name = "TSM_DESCRICAO", nullable = false, unique = true, length = 100)
    private String tsmDescricao;

    @Column(name = "TSM_DT_CADASTRO")
    private LocalDateTime tsmDtCadastro;

    @Column(name = "TSM_DT_ATUALIZACAO")
    private LocalDateTime tsmDtAtualizacao;

    @Column(name = "TSM_ATIVA")
    private Boolean tsmAtiva;
}