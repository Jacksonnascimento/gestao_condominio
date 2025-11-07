package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.EncomendaStatus;
import br.com.gestaocondominio.api.domain.enums.EncomendaTipo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "GC_ENCOMENDA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "encCod")
public class Encomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ENC_COD")
    private Long encCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UNI_COD", nullable = false)
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_REGISTRO", nullable = false)
    private Pessoa pessoaRegistro;

    @Column(name = "ENC_DESTINATARIO", nullable = false, length = 100)
    private String destinatario;

    @Column(name = "ENC_NOME_RECEBIDO_POR", nullable = false, length = 100)
    private String nomeRecebidoPor;

    @Column(name = "ENC_DESCRICAO", length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "ENC_TIPO", nullable = false, length = 50)
    private EncomendaTipo tipo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "ENC_STATUS", nullable = false, length = 20)
    private EncomendaStatus status = EncomendaStatus.PENDENTE;

    @Column(name = "ENC_DT_RECEBIMENTO", nullable = false)
    private LocalDateTime dataRecebimento;

    @Column(name = "ENC_OBSERVACOES", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "ENC_NOME_RETIRADA", length = 100)
    private String nomeRetirada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_RETIRADA")
    private Pessoa pessoaRetirada;

    @Column(name = "ENC_DT_RETIRADA")
    private LocalDateTime dataRetirada;

    @Column(name = "ENC_OBS_ATUALIZACAO", columnDefinition = "TEXT")
    private String observacaoAtualizacao;

    @Column(name = "ENC_DT_ATUALIZACAO_STATUS")
    private LocalDateTime dataAtualizacaoStatus;
}