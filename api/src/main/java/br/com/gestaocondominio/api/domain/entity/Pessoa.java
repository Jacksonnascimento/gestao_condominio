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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GC_PESSOA")
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    
    @Column(name = "PES_IS_GLOBAL_ADMIN")
    private Boolean pesIsGlobalAdmin;
   
}