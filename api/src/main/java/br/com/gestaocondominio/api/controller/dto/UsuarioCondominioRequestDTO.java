package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioCondominioRequestDTO {
    
    private Integer pessoaId;
    private Integer condominioId;
    private UserRole papel;
    private String acaoSenha; // "CRIAR_SENHA" ou "ENVIAR_LINK"

    // Campos para criar nova Pessoa (se pessoaId for null)
    private String pesNome;
    private String pesCpfCnpj;
    private String pesEmail;
    private String pesTelefone;
    private String pesSenhaLogin;
}