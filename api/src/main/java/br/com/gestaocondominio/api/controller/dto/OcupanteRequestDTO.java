package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.TipoPeriodoOcupante;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class OcupanteRequestDTO {

    private Integer condominioId;
    private String tipoCadastro;
    private Integer pessoaId;
    private String pesNome;
    private String pesCpfCnpj;
    private Character pesTipo = 'F';
    private String pesEmail;
    private String pesTelefone;
    private Integer unidadeId;
    private OcupanteVinculo vinculo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate inicioOcupacao;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fimOcupacao;
    
    private String periodoUso;
    private TipoPeriodoOcupante tipoPeriodo;

    public OcupanteRequestDTO(Ocupante ocupante) {
        if (ocupante.getPessoa() != null) {
            this.pessoaId = ocupante.getPessoa().getPesCod();
            this.pesNome = ocupante.getPessoa().getPesNome();
            this.pesCpfCnpj = ocupante.getPessoa().getPesCpfCnpj();
            this.pesTipo = ocupante.getPessoa().getPesTipo();
            this.pesEmail = ocupante.getPessoa().getPesEmail();
            this.pesTelefone = ocupante.getPessoa().getPesTelefone();
        }
        if (ocupante.getUnidade() != null) {
            this.unidadeId = ocupante.getUnidade().getUniCod();
            if (ocupante.getUnidade().getCondominio() != null) {
                this.condominioId = ocupante.getUnidade().getCondominio().getConCod();
            }
        }
        this.vinculo = ocupante.getOcuVinculo();
        this.inicioOcupacao = ocupante.getOcuDtInicioOcupacao();
        this.fimOcupacao = ocupante.getOcuDtFimOcupacao();
        this.periodoUso = ocupante.getOcuPeriodoUso();
        this.tipoPeriodo = ocupante.getOcuTipoPeriodo();
    }
}