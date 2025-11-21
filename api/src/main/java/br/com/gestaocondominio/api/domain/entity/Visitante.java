package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.VisitanteStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "GC_VISITANTE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "visCod")
public class Visitante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VIS_COD")
    private Integer visCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UNI_COD", nullable = false)
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_MORADOR")
    private Pessoa moradorAutorizou;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_REGISTRO", nullable = false)
    private Pessoa pessoaRegistro;

    @Column(name = "VIS_NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "VIS_CPF", length = 14)
    private String cpf;

    @Column(name = "VIS_RG", length = 20)
    private String rg;

    @Column(name = "VIS_TELEFONE", length = 20)
    private String telefone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "VIS_STATUS", nullable = false, length = 20)
    private VisitanteStatus status = VisitanteStatus.NO_LOCAL;

    @Column(name = "VIS_DT_ENTRADA", nullable = false)
    private LocalDateTime dataEntrada;

    @Column(name = "VIS_DT_SAIDA")
    private LocalDateTime dataSaida;

    @Column(name = "VIS_OBSERVACOES", columnDefinition = "TEXT")
    private String observacoes;

    @CreationTimestamp
    @Column(name = "VIS_DT_CADASTRO", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @UpdateTimestamp
    @Column(name = "VIS_DT_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;
}