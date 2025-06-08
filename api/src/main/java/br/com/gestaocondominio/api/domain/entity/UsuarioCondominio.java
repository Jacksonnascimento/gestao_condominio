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
@Table(name = "GC_USUARIO_CONDOMINIO", schema = "dbo")
public class UsuarioCondominio {

    @EmbeddedId 
    private UsuarioCondominioId id;

    
    @ManyToOne
    @JoinColumn(name = "PES_COD", insertable = false, updatable = false)
    private Pessoa pessoa;

    @ManyToOne
    @JoinColumn(name = "CON_COD", insertable = false, updatable = false)
    private Condominio condominio;

    @Column(name = "USC_DT_ASSOCIACAO", nullable = false)
    private LocalDateTime uscDtAssociacao;

    @Column(name = "USC_ATIVO_ASSOCIACAO", nullable = false)
    private Boolean uscAtivoAssociacao;
}