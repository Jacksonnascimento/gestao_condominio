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
@Table(name = "GC_ASSEMBLEIA", schema = "dbo")
public class Assembleia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ASS_COD")
    private Integer assCod;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @Column(name = "ASS_DESCRICAO", nullable = false, length = 100)
    private String assDescricao;

    @Column(name = "ASS_DATA_HORA", nullable = false)
    private LocalDateTime assDataHora;

    @Column(name = "ASS_STATUS", nullable = false, length = 20)
    private String assStatus;

    @Column(name = "ASS_DT_CADASTRO")
    private LocalDateTime assDtCadastro;

    @Column(name = "ASS_DT_ATUALIZACAO")
    private LocalDateTime assDtAtualizacao;
}