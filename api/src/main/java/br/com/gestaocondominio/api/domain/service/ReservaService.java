package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ReservaRequestDTO;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs.OpcoesReserva;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs.ReservaResposta;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Reserva;
import br.com.gestaocondominio.api.domain.enums.ReservaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface ReservaService {
    // Recebem quem está logado e conferem se a pessoa pode ver ou agir na reserva.

    /**
     * Reservas visíveis para a pessoa (as que ela solicitou e as dos condomínios que ela gerencia; todas, para o
     * administrador geral), com os filtros da tela, das mais recentes para as mais antigas.
     */
    Page<ReservaResposta> consultarReservas(Pessoa usuario, Integer conCod, ReservaStatus status, String busca,
                                            Integer areCod, LocalDate dataInicio, LocalDate dataFim,
                                            Pageable pageable);

    /** Totais do painel da tela (total, pendentes e aprovadas), antes dos filtros de situação, área, busca e data. */
    Map<String, Long> contarReservas(Pessoa usuario, Integer conCod);

    /** Uma reserva, se a pessoa a solicitou ou gerencia o condomínio dela. */
    ReservaResposta buscarPorIdDTO(Integer resCod, Pessoa usuario);

    /** Solicita a reserva em nome de quem está logado, para uma unidade em que a pessoa mora. */
    Reserva solicitarReserva(ReservaRequestDTO dto, Pessoa usuario);

    /** Aprova a reserva, se a pessoa gerencia o condomínio dela. */
    Reserva aprovarReserva(Integer resCod, Pessoa usuario);

    /** Rejeita a reserva com o motivo, se a pessoa gerencia o condomínio dela. */
    Reserva rejeitarReserva(Integer resCod, Pessoa usuario, String motivo);

    /** Cancela a reserva, se foi a própria pessoa que a solicitou e ela ainda está pendente ou aprovada. */
    Reserva cancelarReserva(Integer resCod, Pessoa usuario);

    /** Listas de escolha da tela de reservas para a pessoa, opcionalmente só de um condomínio. */
    OpcoesReserva opcoes(Pessoa usuario, Integer conCod);
}
