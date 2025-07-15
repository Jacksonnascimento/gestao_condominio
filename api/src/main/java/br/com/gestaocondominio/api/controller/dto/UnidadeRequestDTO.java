package br.com.gestaocondominio.api.controller.dto;



import br.com.gestaocondominio.api.domain.enums.UnidadeStatusOcupacao;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnidadeRequestDTO {

    private String uniNumero;
    private UnidadeStatusOcupacao uniStatusOcupacao;
    private Integer conCod;
    private Integer utiCod;
    private Boolean uniAtiva;

}