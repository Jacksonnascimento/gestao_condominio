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
@Table(name = "GC_MORADOR", schema = "dbo",
    uniqueConstraints = @UniqueConstraint(columnNames = {"PES_COD", "UNI_COD"})
)
public class Morador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MOR_COD")
    private Integer morCod;

    @ManyToOne
    @JoinColumn(name = "PES_COD", nullable = false)
    private Pessoa pessoa; 

    @ManyToOne
    @JoinColumn(name = "UNI_COD", nullable = false)
    private Unidade unidade; 

    @Column(name = "MOR_TIPO_RELACIONAMENTO", nullable = false, length = 1)
    private Character morTipoRelacionamento; // P - Proprietário, I - Inquilino, etc.

    @Column(name = "MOR_DT_CADASTRO")
    private LocalDateTime morDtCadastro;

    @Column(name = "MOR_DT_ATUALIZACAO")
    private LocalDateTime morDtAtualizacao;
}