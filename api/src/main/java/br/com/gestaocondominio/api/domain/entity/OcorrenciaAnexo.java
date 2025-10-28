package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "GC_OCORRENCIA_ANEXO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "ocaCod")
public class OcorrenciaAnexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OCA_COD")
    private Integer ocaCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OCO_COD", nullable = false)
    private Ocorrencia ocorrencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_ANEXO", nullable = false)
    private Pessoa pessoaAnexo;

    @Column(name = "OCA_CAMINHO_ARQUIVO", nullable = false, length = 555)
    private String caminhoArquivo;

    @Column(name = "OCA_NOME_ORIGINAL", length = 255)
    private String nomeOriginal;

    @Column(name = "OCA_TIPO_ARQUIVO", length = 100)
    private String tipoArquivo; // MIME Type

    @Column(name = "OCA_TAMANHO_ARQUIVO")
    private Long tamanhoArquivo; // Em bytes

    @CreationTimestamp
    @Column(name = "OCA_DT_ANEXO", nullable = false, updatable = false)
    private LocalDateTime dataAnexo;
}