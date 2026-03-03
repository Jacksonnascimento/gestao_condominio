package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GC_RESERVA_CONVIDADO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "rcvCod")
public class ReservaConvidado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RCV_COD")
    private Integer rcvCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RES_COD", nullable = false)
    private Reserva reserva;

    @Column(name = "RCV_NOME", nullable = false, length = 150)
    private String nome;

    @Column(name = "RCV_DOCUMENTO", length = 50)
    private String documento;
}