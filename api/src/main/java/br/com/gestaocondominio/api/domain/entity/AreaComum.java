package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
@EqualsAndHashCode(of = "arcCod")
@ToString(of = {"arcCod", "arcNome"})
@Entity
@Table(name = "gc_area_comum")
public class AreaComum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ARC_COD")
    private Integer arcCod;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @Column(name = "ARC_NOME", nullable = false, length = 100)
    private String arcNome;

    @Column(name = "ARC_DESCRICAO", length = 500)
    private String arcDescricao;

    @Lob
    @Column(name = "ARC_REGRAS_USO", columnDefinition = "TEXT")
    private String arcRegrasUso;

    @Column(name = "ARC_CAPACIDADE_MAXIMA")
    private Integer arcCapacidadeMaxima;

    @Column(name = "ARC_PERMITE_RESERVA", nullable = false)
    private Boolean arcPermiteReserva;

    @Column(name = "ARC_DT_CADASTRO")
    private LocalDateTime arcDtCadastro;

    @Column(name = "ARC_DT_ATUALIZACAO")
    private LocalDateTime arcDtAtualizacao;
}