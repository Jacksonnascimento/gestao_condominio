package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.controller.dto.AreaComumRequestDTO;
import br.com.gestaocondominio.api.controller.dto.AreaComumTurnoDTO;
import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.entity.AreaComumTurno;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Pedidos e respostas de {@code /api/v1/areas-comuns}. */
public final class AreaComumDTOs {

    private AreaComumDTOs() {
    }

    /**
     * Cadastro e edição de uma área comum, com os turnos em que ela pode ser reservada. O {@code condominioId} só é
     * usado no cadastro: a área não muda de condomínio na edição. Na edição, o turno que vem com {@code codigo} é
     * alterado, o que vem sem é criado, e o que não vem é removido (ou recusado, se já tiver reservas).
     */
    public record AreaComumPedido(
            Integer condominioId,
            @NotBlank(message = "Informe o nome da área.")
            @Size(max = 100, message = "O nome da área pode ter até 100 caracteres.") String nome,
            String descricao,
            String termosUso,
            @Min(value = 1, message = "A capacidade máxima precisa ser de pelo menos 1 pessoa.") Integer capacidadeMaxima,
            Boolean permiteConvidados,
            @Min(value = 0, message = "O limite de convidados não pode ser negativo.") Integer limiteConvidados,
            @DecimalMin(value = "0", message = "A taxa de uso não pode ser negativa.") BigDecimal taxaValor,
            @Min(value = 0, message = "A antecedência mínima não pode ser negativa.") Integer diasAntecedenciaMin,
            @Min(value = 1, message = "A antecedência máxima precisa ser de pelo menos 1 dia.") Integer diasAntecedenciaMax,
            Boolean ativa,
            @Valid List<TurnoPedido> turnos) {

        /** Converte para o DTO que o {@code AreaComumService} recebe. */
        public AreaComumRequestDTO paraServico() {
            return AreaComumRequestDTO.builder()
                    .conCod(condominioId)
                    .nome(nome.trim())
                    .descricao(descricao)
                    .termosUso(termosUso)
                    .capacidadeMaxima(capacidadeMaxima)
                    .permiteConvidados(permiteConvidados)
                    .limiteConvidados(limiteConvidados)
                    .taxaValor(taxaValor)
                    .diasAntecedenciaMin(diasAntecedenciaMin)
                    .diasAntecedenciaMax(diasAntecedenciaMax)
                    .ativa(ativa)
                    .turnos(turnos == null ? List.of() : turnos.stream().map(TurnoPedido::paraServico).toList())
                    .build();
        }
    }

    public record TurnoPedido(
            Integer codigo,
            @NotBlank(message = "Informe o nome do turno.")
            @Size(max = 50, message = "O nome do turno pode ter até 50 caracteres.") String nome,
            @NotNull(message = "Informe o horário de início do turno.") LocalTime horaInicio,
            @NotNull(message = "Informe o horário de fim do turno.") LocalTime horaFim,
            Boolean ativo) {

        AreaComumTurnoDTO paraServico() {
            return AreaComumTurnoDTO.builder()
                    .turCod(codigo)
                    .nome(nome.trim())
                    .horaInicio(horaInicio)
                    .horaFim(horaFim)
                    .ativo(ativo)
                    .build();
        }
    }

    /** Área comum com as regras de uso e os turnos. */
    public record AreaComumResposta(Integer codigo, Integer condominioCodigo, String condominioNome, String nome,
                                    String descricao, String termosUso, Integer capacidadeMaxima,
                                    boolean permiteConvidados, Integer limiteConvidados, BigDecimal taxaValor,
                                    Integer diasAntecedenciaMin, Integer diasAntecedenciaMax, boolean ativa,
                                    List<Turno> turnos) {

        /** Todos os turnos, ativos e inativos: é o que o gestor edita. */
        public static AreaComumResposta de(AreaComum area) {
            return de(area, turno -> true);
        }

        /** Só os turnos ativos: é o que quem vai reservar pode escolher. */
        public static AreaComumResposta paraReserva(AreaComum area) {
            return de(area, turno -> !Boolean.FALSE.equals(turno.getAtivo()));
        }

        private static AreaComumResposta de(AreaComum area, Predicate<AreaComumTurno> incluirTurno) {
            List<Turno> turnos = area.getTurnos() == null ? List.of() : area.getTurnos().stream()
                    .filter(incluirTurno)
                    .sorted(Comparator.comparing(AreaComumTurno::getHoraInicio))
                    .map(Turno::de)
                    .toList();
            Condominio condominio = area.getCondominio();
            return new AreaComumResposta(area.getAreCod(), condominio.getConCod(), condominio.getConNome(),
                    area.getNome(), area.getDescricao(), area.getTermosUso(), area.getCapacidadeMaxima(),
                    Boolean.TRUE.equals(area.getPermiteConvidados()), area.getLimiteConvidados(), area.getTaxaValor(),
                    area.getDiasAntecedenciaMin(), area.getDiasAntecedenciaMax(), Boolean.TRUE.equals(area.getAtiva()),
                    turnos);
        }
    }

    public record Turno(Integer codigo, String nome, LocalTime horaInicio, LocalTime horaFim, boolean ativo) {

        static Turno de(AreaComumTurno turno) {
            return new Turno(turno.getTurCod(), turno.getNome(), turno.getHoraInicio(), turno.getHoraFim(),
                    !Boolean.FALSE.equals(turno.getAtivo()));
        }
    }

    /** Condomínio numa lista de escolha (filtro da listagem ou formulário de cadastro). */
    public record CondominioOpcao(Integer codigo, String nome) {

        public static CondominioOpcao de(Condominio condominio) {
            return new CondominioOpcao(condominio.getConCod(), condominio.getConNome());
        }
    }
}
