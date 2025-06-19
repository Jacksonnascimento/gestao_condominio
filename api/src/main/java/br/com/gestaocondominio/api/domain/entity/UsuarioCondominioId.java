package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsuarioCondominioId implements Serializable {

    private Integer pesCod;
    private Integer conCod;
    private UserRole uscPapel;
}