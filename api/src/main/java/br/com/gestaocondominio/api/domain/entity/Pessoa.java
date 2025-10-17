package br.com.gestaocondominio.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pesCod")
@ToString(of = {"pesCod", "pesNome", "pesEmail"})
@Entity
@Table(name = "gc_pessoa")
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PES_COD")
    private Integer pesCod;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "PES_NOME", nullable = false, length = 100)
    private String pesNome;

    @Column(name = "PES_CPF_CNPJ", unique = true, nullable = false, length = 14)
    private String pesCpfCnpj;

    @Column(name = "PES_TIPO", length = 1)
    private Character pesTipo;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "PES_EMAIL", unique = true, nullable = false, length = 100)
    private String pesEmail;

    @Column(name = "PES_TELEFONE", length = 20)
    private String pesTelefone;

    @Column(name = "PES_TELEFONE2", length = 20)
    private String pesTelefone2;

    @Column(name = "PES_ATIVO")
    private Boolean pesAtivo;

    @Column(name = "PES_DT_CADASTRO")
    private LocalDateTime pesDtCadastro;

    @Column(name = "PES_DT_ATUALIZACAO")
    private LocalDateTime pesDtAtualizacao;

    @JsonIgnore
    @Column(name = "PES_SENHA_LOGIN", length = 255)
    private String pesSenhaLogin;

    @Column(name = "PES_IS_GLOBAL_ADMIN")
    private Boolean pesIsGlobalAdmin;

    @JsonIgnore
    @Lob
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "PES_IMAGEM", columnDefinition = "BYTEA")
    private byte[] pesImagem;
}