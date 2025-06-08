package br.com.gestaocondominio.api.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AssembleiaVotoId implements Serializable {
    private Integer aspCod; 
    private Integer pesCod;
}