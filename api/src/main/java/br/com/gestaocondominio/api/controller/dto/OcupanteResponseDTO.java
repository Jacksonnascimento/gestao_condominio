package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.TipoPeriodoOcupante;

import java.time.LocalDate;

public record OcupanteResponseDTO(
    Integer id,
    Integer pessoaId,
    String nomeCompleto,
    String email,
    String telefone,
    OcupanteVinculo vinculo,
    String unidadeNumero,
    String unidadeBloco,
    String condominioNome, 
    LocalDate inicioOcupacao,
    LocalDate fimOcupacao,
    String periodoUso,
    TipoPeriodoOcupante tipoPeriodo
) {
    public OcupanteResponseDTO(Ocupante ocupante) {
        this(
            ocupante.getOcuCod(),
            ocupante.getPessoa().getPesCod(),
            ocupante.getPessoa().getPesNome(),
            ocupante.getPessoa().getPesEmail(),
            ocupante.getPessoa().getPesTelefone(),
            ocupante.getOcuVinculo(),
            ocupante.getUnidade().getUniNumero(),
            ocupante.getUnidade().getBloco(),
            ocupante.getUnidade().getCondominio().getConNome(),
            ocupante.getOcuDtInicioOcupacao(),
            ocupante.getOcuDtFimOcupacao(),
            ocupante.getOcuPeriodoUso(),
            ocupante.getOcuTipoPeriodo()
        );
    }
}