package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Comunicado;
import br.com.gestaocondominio.api.domain.enums.PublicoDestino;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class ComunicadoRequestDTO {

    private Integer id;
    private String titulo;
    private String mensagem;
    private PublicoDestino publicoDestino;
    private Boolean isUrgente;
    private List<Integer> condominioIds;

    public ComunicadoRequestDTO(Comunicado comunicado) {
        this.id = comunicado.getComId();
        this.titulo = comunicado.getTitulo();
        this.mensagem = comunicado.getMensagem();
        this.publicoDestino = comunicado.getPublicoDestino();
        this.isUrgente = comunicado.getIsUrgente();
        
        if (comunicado.getCondominios() != null) {
            this.condominioIds = comunicado.getCondominios().stream()
                    .map(cond -> cond.getConCod())
                    .collect(Collectors.toList());
        }
    }
}