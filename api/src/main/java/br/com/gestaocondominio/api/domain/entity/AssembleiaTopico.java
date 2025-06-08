package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_ASSEMBLEIA_TOPICO", schema = "dbo")
public class AssembleiaTopico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ASP_COD")
    private Integer aspCod;

    @Column(name = "ASP_DESCRICAO", nullable = false, length = 100)
    private String aspDescricao;

    @ManyToOne
    @JoinColumn(name = "ASS_COD", nullable = false)
    private Assembleia assembleia;
}