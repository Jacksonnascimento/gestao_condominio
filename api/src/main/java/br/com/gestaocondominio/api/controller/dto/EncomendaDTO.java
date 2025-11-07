package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Encomenda;
import br.com.gestaocondominio.api.domain.enums.EncomendaStatus;
import br.com.gestaocondominio.api.domain.enums.EncomendaTipo;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EncomendaDTO {

    private Long id;
    private String condominioNome;
    private String unidadeNumero;
    private String unidadeBloco;
    private String destinatario;
    private EncomendaTipo tipo;
    private String tipoDescricao;
    private String descricao;
    private EncomendaStatus status;
    private String statusDescricao;
    private LocalDateTime dataRecebimento;
    private String nomeRecebidoPor;
    private String observacoes;
    private LocalDateTime dataRetirada;
    private String nomeRetirada;
    private String observacaoAtualizacao;

    public EncomendaDTO(Encomenda encomenda) {
        this.id = encomenda.getEncCod();
        this.destinatario = encomenda.getDestinatario();
        this.tipo = encomenda.getTipo();
        this.tipoDescricao = encomenda.getTipo() != null ? encomenda.getTipo().getDescricao() : null;
        this.descricao = encomenda.getDescricao();
        this.status = encomenda.getStatus();
        this.statusDescricao = encomenda.getStatus() != null ? encomenda.getStatus().getDescricao() : null;
        this.dataRecebimento = encomenda.getDataRecebimento();
        this.nomeRecebidoPor = encomenda.getNomeRecebidoPor();
        this.observacoes = encomenda.getObservacoes();
        this.dataRetirada = encomenda.getDataRetirada();
        this.nomeRetirada = encomenda.getNomeRetirada();
        this.observacaoAtualizacao = encomenda.getObservacaoAtualizacao();

        if (encomenda.getCondominio() != null) {
            this.condominioNome = encomenda.getCondominio().getConNome();
        }
        if (encomenda.getUnidade() != null) {
            this.unidadeNumero = encomenda.getUnidade().getUniNumero();
            this.unidadeBloco = encomenda.getUnidade().getBloco();
        }
    }
}