package br.com.gestaocondominio.api.domain.enums;

import lombok.Getter;

@Getter
public enum UserRole {
 
    SINDICO("Síndico"),
    MORADOR("Morador"),
    FUNCIONARIO_ADM("Funcionário adm."),
    PORTEIRO("Porteiro"),
    ADMIN("Administrador"); 

    private final String descricao;

    UserRole(String descricao) {
        this.descricao = descricao;
    }
}