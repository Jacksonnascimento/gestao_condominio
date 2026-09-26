package br.com.gestaocondominio.api.controller.v1.dto;

import br.com.gestaocondominio.api.controller.dto.ReservaConvidadoDTO;
import br.com.gestaocondominio.api.controller.dto.ReservaRequestDTO;
import br.com.gestaocondominio.api.controller.v1.dto.AreaComumDTOs.AreaComumResposta;
import br.com.gestaocondominio.api.controller.v1.dto.AreaComumDTOs.CondominioOpcao;
import br.com.gestaocondominio.api.domain.entity.AreaComumTurno;
import br.com.gestaocondominio.api.domain.entity.Reserva;
import br.com.gestaocondominio.api.domain.entity.ReservaConvidado;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.ReservaStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Pedidos e respostas de {@code /api/v1/reservas}. */
public final class ReservaDTOs {

    private ReservaDTOs() {
    }

    /** Textos das situações, os mesmos das etiquetas da tela de reservas. */
    public static String descricaoDo(ReservaStatus status) {
        return switch (status) {
            case PENDENTE_APROVACAO -> "Pendente";
            case APROVADA -> "Aprovada";
            case REJEITADA -> "Rejeitada";
            case CANCELADA_PELO_MORADOR -> "Cancelada";
            case CONCLUIDA -> "Concluída";
        };
    }

    /**
     * Solicitação de reserva feita por quem está logado, para uma unidade em que mora. Sem turno, a reserva é do dia
     * inteiro. Convidados só entram se a área permitir, até o limite dela.
     */
    public record SolicitarReservaPedido(
            @NotNull(message = "Informe a área comum.") Integer areaId,
            Integer turnoId,
            @NotNull(message = "Informe a unidade.") Integer unidadeId,
            @NotNull(message = "Informe a data da reserva.") LocalDate data,
            @NotNull(message = "É obrigatório aceitar os termos de uso.")
            @AssertTrue(message = "É obrigatório aceitar os termos de uso.") Boolean termosAceitos,
            @Valid List<ConvidadoPedido> convidados) {

        /** Converte para o DTO que o {@code ReservaService} recebe. */
        public ReservaRequestDTO paraServico() {
            return ReservaRequestDTO.builder()
                    .areCod(areaId)
                    .turCod(turnoId)
                    .uniCod(unidadeId)
                    .data(data)
                    .termosAceitos(termosAceitos)
                    .convidados(convidados == null ? List.of() : convidados.stream()
                            .map(c -> ReservaConvidadoDTO.builder()
                                    .nome(c.nome().trim())
                                    .documento(c.documento() == null || c.documento().isBlank()
                                            ? null : c.documento().trim())
                                    .build())
                            .toList())
                    .build();
        }
    }

    public record ConvidadoPedido(
            @NotBlank(message = "Informe o nome do convidado.")
            @Size(max = 150, message = "O nome do convidado pode ter até 150 caracteres.") String nome,
            @Size(max = 50, message = "O documento do convidado pode ter até 50 caracteres.") String documento) {
    }

    public record RejeitarReservaPedido(@NotBlank(message = "Informe o motivo da rejeição.") String motivo) {
    }

    /**
     * Reserva como aparece no cartão da tela. {@code podeAprovarOuRejeitar} e {@code podeCancelar} dizem quais botões
     * mostrar para quem está logado.
     */
    public record ReservaResposta(Integer codigo, ReservaStatus status, String statusDescricao, LocalDate data,
                                  Integer areaCodigo, String areaNome, Integer condominioCodigo,
                                  String condominioNome, Integer turnoCodigo, String turnoNome,
                                  LocalTime turnoHoraInicio, LocalTime turnoHoraFim, Integer unidadeCodigo,
                                  String unidadeNumero, String unidadeBloco, Integer solicitanteCodigo,
                                  String solicitanteNome, String motivoRejeicao, LocalDateTime dataRegistro,
                                  List<Convidado> convidados, boolean podeAprovarOuRejeitar,
                                  boolean podeCancelar) {

        public static ReservaResposta de(Reserva reserva, boolean podeAprovarOuRejeitar, boolean podeCancelar) {
            AreaComumTurno turno = reserva.getTurno();
            Unidade unidade = reserva.getUnidade();
            return new ReservaResposta(reserva.getResCod(), reserva.getStatus(), descricaoDo(reserva.getStatus()),
                    reserva.getData(), reserva.getAreaComum().getAreCod(), reserva.getAreaComum().getNome(),
                    reserva.getAreaComum().getCondominio().getConCod(),
                    reserva.getAreaComum().getCondominio().getConNome(),
                    turno == null ? null : turno.getTurCod(), turno == null ? null : turno.getNome(),
                    turno == null ? null : turno.getHoraInicio(), turno == null ? null : turno.getHoraFim(),
                    unidade.getUniCod(), unidade.getUniNumero(), unidade.getBloco(),
                    reserva.getMorador().getPesCod(), reserva.getMorador().getPesNome(),
                    reserva.getMotivoRejeicao(), reserva.getDataRegistro(),
                    reserva.getConvidados() == null ? List.of()
                            : reserva.getConvidados().stream().map(Convidado::de).toList(),
                    podeAprovarOuRejeitar, podeCancelar);
        }
    }

    public record Convidado(Integer codigo, String nome, String documento) {

        static Convidado de(ReservaConvidado convidado) {
            return new Convidado(convidado.getRcvCod(), convidado.getNome(), convidado.getDocumento());
        }
    }

    /** Unidade de quem está logado, para escolher no formulário de solicitação. */
    public record UnidadeOpcao(Integer codigo, String numero, String bloco, Integer condominioCodigo,
                               String condominioNome) {

        public static UnidadeOpcao de(Unidade unidade) {
            return new UnidadeOpcao(unidade.getUniCod(), unidade.getUniNumero(), unidade.getBloco(),
                    unidade.getCondominio().getConCod(), unidade.getCondominio().getConNome());
        }
    }

    /** Área comum no filtro da listagem. */
    public record AreaOpcao(Integer codigo, String nome, Integer condominioCodigo) {
    }

    /**
     * Ocupação de uma área comum num dia, para escolher a data e o turno antes de pedir a reserva. Não diz quem
     * reservou. O dia inteiro só está livre sem nenhuma reserva na data; um turno, sem reserva no mesmo turno nem do
     * dia inteiro. Contam as reservas pendentes, aprovadas e concluídas.
     */
    public record DisponibilidadeDoDia(LocalDate data, boolean diaInteiroLivre, List<TurnoDoDia> turnos) {
    }

    /** Turno ativo da área, e se ele está livre no dia. */
    public record TurnoDoDia(Integer codigo, String nome, LocalTime horaInicio, LocalTime horaFim, boolean livre) {
    }

    /**
     * Listas de escolha da tela de reservas. {@code areasParaSolicitar} traz as áreas ativas dos condomínios das
     * unidades de quem está logado, com termos de uso, taxa, regras de convidados e turnos ativos, que é o que o
     * formulário de solicitação mostra.
     */
    public record OpcoesReserva(List<Opcao> status, List<CondominioOpcao> condominios, List<UnidadeOpcao> unidades,
                                List<AreaComumResposta> areasParaSolicitar, List<AreaOpcao> areasParaFiltro,
                                boolean podeGerenciar, boolean podeSolicitar) {
    }
}
