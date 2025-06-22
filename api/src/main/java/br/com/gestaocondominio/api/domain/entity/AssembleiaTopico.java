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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "aspCod")
@ToString(of = {"aspCod", "aspDescricao"})
@Entity
@Table(name = "gc_assembleia_topico")
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