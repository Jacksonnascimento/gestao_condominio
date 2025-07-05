package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Pessoa;

public record LoginResponse(String token, Pessoa user) {
}