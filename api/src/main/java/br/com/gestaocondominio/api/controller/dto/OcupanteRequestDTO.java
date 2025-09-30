package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.TipoPeriodoOcupante;
import java.time.LocalDate;

public record OcupanteRequestDTO(
  
    String tipoCadastro,

    
    Integer pessoaId,
    String pesNome,
    String pesCpfCnpj,
    Character pesTipo,
    String pesEmail,
    String pesTelefone,

  
    Integer unidadeId,
    OcupanteVinculo vinculo,
    LocalDate inicioOcupacao,
    LocalDate fimOcupacao,
    String periodoUso,
    TipoPeriodoOcupante tipoPeriodo
) {}