package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@EqualsAndHashCode(of = "docCod")
@ToString(of = {"docCod", "nome", "tipo"})
@Entity
@Table(name = "gc_documento")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DOC_COD")
    private Integer docCod;

    @ManyToOne
    @JoinColumn(name = "CON_COD", nullable = false)
    private Condominio condominio;

    @ManyToOne
    @JoinColumn(name = "ASS_COD")
    private Assembleia assembleia;

    @ManyToOne
    @JoinColumn(name = "PES_COD_UPLOAD", nullable = false)
    private Pessoa uploader;

    @Column(name = "DOC_NOME", nullable = false, length = 150)
    private String nome;

    @Column(name = "DOC_TIPO", nullable = false, length = 50)
    private String tipo;

    @Column(name = "DOC_CAMINHO_ARQUIVO", nullable = false, length = 500)
    private String caminhoArquivo;

    @Column(name = "DOC_PERMISSAO_VISUALIZAR", nullable = false, length = 1)
    private Character permissaoVisualizar;

    @Column(name = "DOC_DT_UPLOAD")
    private LocalDateTime dtUpload;

    @Column(name = "DOC_DT_ATUALIZACAO")
    private LocalDateTime dtAtualizacao;
}