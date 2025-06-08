package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable // Indica que esta classe pode ser embutida em outra entidade
public class AssembleiaParticipanteId implements Serializable {
    private Integer assCod; // Nome deve ser igual ao do atributo na entidade principal
    private Integer pesCod; // Nome deve ser igual ao do atributo na entidade principal
}