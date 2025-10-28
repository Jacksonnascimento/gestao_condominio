package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.enums.OcorrenciaTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class OcorrenciaRequestDTO {

    

    @NotNull(message = "A unidade é obrigatória.")
    private Integer unidadeId;

    @NotNull(message = "O tipo da ocorrência é obrigatório.")
    private OcorrenciaTipo tipo = OcorrenciaTipo.BARULHO; 

    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 150, message = "O título não pode exceder 150 caracteres.")
    private String titulo;

    @NotBlank(message = "A descrição é obrigatória.")
    private String descricao;

    
    private Integer condominioId;
}