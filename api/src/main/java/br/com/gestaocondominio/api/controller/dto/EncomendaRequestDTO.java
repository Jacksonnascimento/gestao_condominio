package br.com.gestaocondominio.api.controller.dto;

import br.com.gestaocondominio.api.domain.enums.EncomendaTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class EncomendaRequestDTO {

    @NotNull(message = "O condomínio é obrigatório.")
    private Integer condominioId;

    @NotNull(message = "A unidade é obrigatória.")
    private Integer unidadeId;

    @NotBlank(message = "O destinatário é obrigatório.")
    private String destinatario;

    @NotNull(message = "O tipo é obrigatório.")
    private EncomendaTipo tipo;

    private String descricao;

    @NotNull(message = "A data de recebimento é obrigatória.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataRecebimento;

    @NotNull(message = "A hora do recebimento é obrigatória.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horaRecebimento;

    @NotBlank(message = "O campo 'Recebido por' é obrigatório.")
    private String nomeRecebidoPor;

    private String observacoes;
}