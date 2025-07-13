package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "utiCod")
@ToString(of = {"utiCod", "utiDescricao"})
@Entity
@Table(name = "gc_unidade_tipo")
public class UnidadeTipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UTI_COD")
    private Integer utiCod;

    @Column(name = "UTI_DESCRICAO", nullable = false, length = 100)
    private String utiDescricao;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;
}