package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.StatusContrato;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GC_CONTRATO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CTR_COD")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @Column(name = "CTR_EMPRESA", nullable = false, length = 100)
    private String empresa;

    @Column(name = "CTR_SERVICO", nullable = false, length = 255)
    private String servico;

    @Column(name = "CTR_VALOR", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "CTR_RESPONSAVEL", length = 100)
    private String responsavel;

    @Enumerated(EnumType.STRING)
    @Column(name = "CTR_STATUS", nullable = false, length = 50)
    private StatusContrato status;

    @Column(name = "CTR_DATA_INICIO", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "CTR_DATA_FIM", nullable = false)
    private LocalDate dataFim;

    @Column(name = "CTR_OBSERVACOES", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "CTR_DT_CADASTRO", updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "CTR_DT_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    public void prePersist() {
        dataCadastro = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}