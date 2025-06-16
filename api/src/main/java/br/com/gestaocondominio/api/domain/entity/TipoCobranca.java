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
@Table(name = "GC_TIPO_COBRANCA", schema = "dbo")
public class TipoCobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TIC_COD")
    private Integer ticCod;

    @Column(name = "TIC_DESCRICAO", nullable = false, length = 100)
    private String ticDescricao;

    @Column(name = "TIC_DT_CADASTRO")
    private LocalDateTime ticDtCadastro;

    @Column(name = "TIC_DT_ATUALIZACAO")
    private LocalDateTime ticDtAtualizacao;

    @Column(name = "TIC_ATIVA") 
    private Boolean ticAtiva;
}