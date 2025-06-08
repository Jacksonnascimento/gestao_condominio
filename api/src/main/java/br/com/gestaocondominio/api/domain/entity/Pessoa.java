package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data // Anotação do Lombok: gera getters, setters, toString, equals e hashCode
@NoArgsConstructor // Anotação do Lombok: gera um construtor sem argumentos
@AllArgsConstructor // Anotação do Lombok: gera um construtor com todos os argumentos

@Entity // Indica que esta classe é uma entidade JPA (representa uma tabela)
@Table(name = "GC_PESSOA") // Especifica o nome da tabela no banco de dados
public class Pessoa {

    @Id // Marca o campo como a chave primária da tabela
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configura a geração automática do ID (compatível com IDENTITY do SQL Server)
    @Column(name = "PES_COD")
    private Integer pesCod;

    @Column(name = "PES_NOME", nullable = false, length = 100)
    private String pesNome;

    @Column(name = "PES_CPF_CNPJ", unique = true, nullable = false, length = 14)
    private String pesCpfCnpj;

    @Column(name = "PES_TIPO", length = 1)
    private Character pesTipo;

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

    @Column(name = "PES_SENHA_LOGIN", length = 255)
    private String pesSenhaLogin;

}