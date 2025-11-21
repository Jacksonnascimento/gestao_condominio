package br.com.gestaocondominio.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VisitanteRequestDTO {

    @NotBlank(message = "O nome do visitante é obrigatório.")
    private String nome;

    private String cpf;
    private String rg;
    private String telefone;

    @NotNull(message = "A unidade é obrigatória.")
    private Integer unidadeId;

    private Integer moradorId;
    private String observacoes;
    
    private Integer condominioId;
}