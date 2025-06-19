package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.AssembleiaStatus;
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

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "assCod")
@ToString(of = {"assCod", "assDescricao", "assStatus"})
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

    @Enumerated(EnumType.STRING)
    @Column(name = "ASS_STATUS", nullable = false, length = 50)
    private AssembleiaStatus assStatus;

    @Column(name = "ASS_DT_CADASTRO")
    private LocalDateTime assDtCadastro;

    @Column(name = "ASS_DT_ATUALIZACAO")
    private LocalDateTime assDtAtualizacao;

    @Column(name = "ASS_ATIVA")
    private Boolean assAtiva;
}