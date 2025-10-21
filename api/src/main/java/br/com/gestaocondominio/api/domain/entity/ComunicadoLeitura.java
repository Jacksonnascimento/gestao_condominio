package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "GC_COMUNICADO_LEITURA")
@Getter
@Setter
@NoArgsConstructor
public class ComunicadoLeitura {

    @EmbeddedId
    private ComunicadoLeituraId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("comunicadoId")
    @JoinColumn(name = "COM_COD")
    private Comunicado comunicado;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pessoaId")
    @JoinColumn(name = "PES_COD")
    private Pessoa pessoa;

    @CreationTimestamp
    @Column(name = "CLE_DT_LEITURA", nullable = false, updatable = false)
    private LocalDateTime dataLeitura;

    public ComunicadoLeitura(Comunicado comunicado, Pessoa pessoa) {
        
        this.id = new ComunicadoLeituraId(comunicado.getComId(), pessoa.getPesCod());
        this.comunicado = comunicado;
        this.pessoa = pessoa;
    }
}