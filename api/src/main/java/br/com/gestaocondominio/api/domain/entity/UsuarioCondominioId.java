package br.com.gestaocondominio.api.domain.entity;

import br.com.gestaocondominio.api.domain.enums.UserRole; 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Objects;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCondominioId implements Serializable {

   
    private Integer pesCod;
    private Integer conCod;
    private UserRole uscPapel;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioCondominioId that = (UsuarioCondominioId) o;
        return Objects.equals(pesCod, that.pesCod) &&
               Objects.equals(conCod, that.conCod) &&
               Objects.equals(uscPapel, that.uscPapel); 
    }

    @Override
    public int hashCode() {
        return Objects.hash(pesCod, conCod, uscPapel);
    }
}