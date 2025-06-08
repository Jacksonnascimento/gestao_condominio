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
@Table(name = "GC_GESTAO_COMUNICACAO", schema = "dbo")
public class GestaoComunicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COM_COD")
    private Integer comCod;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @ManyToOne
    @JoinColumn(name = "COM_REMETENTE", nullable = false)
    private Pessoa remetente;

    @Column(name = "COM_DES_TODOS", length = 1)
    private Character comDesTodos;

    @Column(name = "COM_ASSUNTO", nullable = false, length = 50)
    private String comAssunto;

    @Column(name = "COM_MENSAGEM", nullable = false, length = 500)
    private String comMensagem;

    @Column(name = "COM_TIPO_NOTIFICACAO", length = 1)
    private Character comTipoNotificacao;

    @Column(name = "COM_DT_CADASTRO")
    private LocalDateTime comDtCadastro;

    @Column(name = "COM_DT_ATUALIZACAO")
    private LocalDateTime comDtAtualizacao;

    @Column(name = "COM_DT_ENVIO")
    private LocalDateTime comDtEnvio;
}