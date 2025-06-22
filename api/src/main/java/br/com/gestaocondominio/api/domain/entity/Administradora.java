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
@EqualsAndHashCode(of = "admCod")
@ToString(exclude = {"dadosEmpresa", "responsavel"})
@Entity
@Table(name = "gc_administradora")
public class Administradora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADM_COD")
    private Integer admCod;

    @ManyToOne
    @JoinColumn(name = "PES_COD", nullable = false)
    private Pessoa dadosEmpresa;

    @ManyToOne
    @JoinColumn(name = "ADM_RESPONSAVEL", nullable = false)
    private Pessoa responsavel;

    @Column(name = "ADM_DT_CADASTRO")
    private LocalDateTime admDtCadastro;

    @Column(name = "ADM_DT_ATUALIZACAO")
    private LocalDateTime admDtAtualizacao;

    @Column(name = "ADM_ATIVO")
    private Boolean admAtivo;
}