package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.UserRole; // Importar o novo ENUM do pacote 'enums'
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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