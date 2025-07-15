// src/main/java/br/com/gestaocondominio/api/domain/entity/DespesaCategoria.java
package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn; 
import jakarta.persistence.ManyToOne;  
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;  
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
@EqualsAndHashCode(of = "dcaCod")
@ToString(of = {"dcaCod", "dcaDescricao", "dcaAtiva"})
@Entity
@Table(name = "gc_despesa_categoria",
       uniqueConstraints = @UniqueConstraint(columnNames = {"DCA_DESCRICAO", "CON_COD"}) // Garante descrição única por condomínio
)
public class DespesaCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DCA_COD")
    private Integer dcaCod;

    @ManyToOne 
    @JoinColumn(name = "CON_COD", nullable = false) 
    private Condominio condominio;

    @Column(name = "DCA_DESCRICAO", nullable = false, length = 100)
    private String dcaDescricao;

    @Column(name = "DCA_ATIVA", nullable = false)
    private Boolean dcaAtiva;
}