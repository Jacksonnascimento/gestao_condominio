package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.MoradorType;
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
@EqualsAndHashCode(of = "morCod")
@ToString(of = {"morCod", "morTipoRelacionamento"})
@Entity
@Table(name = "gc_morador")
public class Morador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MOR_COD")
    private Integer morCod;

    @ManyToOne
    @JoinColumn(name = "PES_COD", nullable = false)
    private Pessoa pessoa;

    @ManyToOne
    @JoinColumn(name = "UNI_COD", nullable = false)
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "MOR_TIPO_RELACIONAMENTO", nullable = false, length = 50)
    private MoradorType morTipoRelacionamento;

    @Column(name = "MOR_DT_CADASTRO", nullable = false)
    private LocalDateTime morDtCadastro;

    @Column(name = "MOR_DT_ATUALIZACAO")
    private LocalDateTime morDtAtualizacao;
}