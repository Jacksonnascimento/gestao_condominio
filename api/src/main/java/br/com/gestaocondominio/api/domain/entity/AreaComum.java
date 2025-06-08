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
@Table(name = "GC_AREA_COMUM", schema = "dbo")
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
    @Column(name = "ARC_REGRAS_USO")
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