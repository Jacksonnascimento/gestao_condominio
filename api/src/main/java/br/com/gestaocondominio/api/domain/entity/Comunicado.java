package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "GC_COMUNICADO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comunicado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COM_COD")
    private Integer comId;

    @Column(name = "COM_TITULO", nullable = false)
    private String titulo;

    @Column(name = "COM_MENSAGEM", nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "COM_PUBLICO_DESTINO", nullable = false)
    private PublicoDestino publicoDestino;

    @Column(name = "COM_IS_URGENTE", nullable = false)
    private Boolean isUrgente;

    @Column(name = "COM_CAMINHO_ANEXO")
    private String caminhoAnexo;

    @CreationTimestamp
    @Column(name = "COM_DT_CADASTRO", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PES_COD_CRIADOR", nullable = false)
    private Pessoa criador;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "GC_COMUNICADO_CONDOMINIO",
        joinColumns = @JoinColumn(name = "COM_COD"),
        inverseJoinColumns = @JoinColumn(name = "CON_COD")
    )
    private Set<Condominio> condominios;
}