package br.com.gestaocondominio.api.domain.enums;

import lombok.Getter;

@Getter 
public enum PublicoDestino {
    TODOS("Todos"), 
    PROPRIETARIOS("Proprietários"),
    INQUILINOS("Inquilinos"),
    FUNCIONARIOS("Funcionários");

    private final String descricao; 

    PublicoDestino(String descricao) { 
        this.descricao = descricao;
    }

    
}