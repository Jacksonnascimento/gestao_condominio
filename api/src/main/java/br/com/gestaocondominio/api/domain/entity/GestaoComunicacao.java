package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.ComunicadoDestino;
import br.com.gestaocondominio.api.domain.enums.ComunicadoTipoNotificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "comCod")
@ToString(of = {"comCod", "comAssunto"})
@Entity
@Table(name = "gc_gestao_comunicacao")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "COM_DES_TODOS", nullable = false, length = 50)
    private ComunicadoDestino comDesTodos;

    @Column(name = "COM_ASSUNTO", nullable = false, length = 50)
    private String comAssunto;

    @Column(name = "COM_MENSAGEM", nullable = false, length = 500)
    private String comMensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "COM_TIPO_NOTIFICACAO", length = 50)
    private ComunicadoTipoNotificacao comTipoNotificacao;

    @Column(name = "COM_DT_CADASTRO", nullable = false)
    private LocalDateTime comDtCadastro;

    @Column(name = "COM_DT_ATUALIZACAO")
    private LocalDateTime comDtAtualizacao;

    @Column(name = "COM_DT_ENVIO")
    private LocalDateTime comDtEnvio;
}