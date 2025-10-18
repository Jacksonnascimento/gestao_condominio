package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.entity.Contrato;
import br.com.gestaocondominio.api.domain.enums.StatusContrato;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ContratoRequestDTO {

    private Long id;
    private Integer condominioId;
    private String empresa;
    private String servico;
    private BigDecimal valor;
    private String responsavel;
    private StatusContrato status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;
    
    private String observacoes;

    public void fromEntity(Contrato contrato) {
        this.id = contrato.getId();
        this.condominioId = contrato.getCondominio().getConCod();
        this.empresa = contrato.getEmpresa();
        this.servico = contrato.getServico();
        this.valor = contrato.getValor();
        this.responsavel = contrato.getResponsavel();
        this.status = contrato.getStatus();
        this.dataInicio = contrato.getDataInicio();
        this.dataFim = contrato.getDataFim();
        this.observacoes = contrato.getObservacoes();
    }
}