package br.com.gestaocondominio.api.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record PessoaUpdateRequest(
    String pesNome,
    String pesCpfCnpj,
    Character pesTipo,
    String pesEmail,
    String pesTelefone,
    String pesTelefone2,
    String pesSenhaLogin
) {}