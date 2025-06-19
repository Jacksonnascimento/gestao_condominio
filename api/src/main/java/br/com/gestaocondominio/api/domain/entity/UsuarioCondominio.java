package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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
@EqualsAndHashCode(of = {"pesCod", "conCod", "uscPapel"})
@ToString(of = {"pesCod", "conCod", "uscPapel"})
@Entity
@Table(name = "GC_USUARIO_CONDOMINIO", schema = "dbo")
@IdClass(UsuarioCondominioId.class)
public class UsuarioCondominio {

    @Id
    @Column(name = "PES_COD", nullable = false)
    private Integer pesCod;

    @Id
    @Column(name = "CON_COD", nullable = false)
    private Integer conCod;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "USC_PAPEL", nullable = false, length = 50)
    private UserRole uscPapel;

    @ManyToOne
    @JoinColumn(name = "PES_COD", referencedColumnName = "PES_COD", insertable = false, updatable = false)
    private Pessoa pessoa;

    @ManyToOne
    @JoinColumn(name = "CON_COD", referencedColumnName = "CON_COD", insertable = false, updatable = false)
    private Condominio condominio;

    @Column(name = "USC_DT_ASSOCIACAO", nullable = false)
    private LocalDateTime uscDtAssociacao;

    @Column(name = "USC_ATIVO_ASSOCIACAO", nullable = false)
    private Boolean uscAtivoAssociacao;

    @Column(name = "USC_DT_ATUALIZACAO")
    private LocalDateTime uscDtAtualizacao;
}