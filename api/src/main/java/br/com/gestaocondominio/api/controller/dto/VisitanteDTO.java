package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Visitante;
import br.com.gestaocondominio.api.domain.enums.VisitanteStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class VisitanteDTO {

    private Integer id;
    private String nome;
    private String cpf;
    private String rg;
    private String telefone;
    private String unidadeNumero;
    private String unidadeBloco;
    private String moradorNome;
    private VisitanteStatus status;
    private String statusDescricao;
    private LocalDateTime dataEntrada;
    private LocalDateTime dataSaida;
    private String observacoes;
    private String condominioNome;

    public VisitanteDTO(Visitante visitante) {
        this.id = visitante.getVisCod();
        this.nome = visitante.getNome();
        this.cpf = visitante.getCpf();
        this.rg = visitante.getRg();
        this.telefone = visitante.getTelefone();
        this.status = visitante.getStatus();
        this.statusDescricao = visitante.getStatus() != null ? visitante.getStatus().getDescricao() : "";
        this.dataEntrada = visitante.getDataEntrada();
        this.dataSaida = visitante.getDataSaida();
        this.observacoes = visitante.getObservacoes();

        if (visitante.getUnidade() != null) {
            this.unidadeNumero = visitante.getUnidade().getUniNumero();
            this.unidadeBloco = visitante.getUnidade().getBloco();
        }

        if (visitante.getMoradorAutorizou() != null) {
            this.moradorNome = visitante.getMoradorAutorizou().getPesNome();
        }

        if (visitante.getCondominio() != null) {
            this.condominioNome = visitante.getCondominio().getConNome();
        }
    }
}