package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.v1.dto.Pagina;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs.OpcoesReserva;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs.RejeitarReservaPedido;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs.ReservaResposta;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs.SolicitarReservaPedido;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Reserva;
import br.com.gestaocondominio.api.domain.enums.ReservaStatus;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * Reservas das áreas comuns. Quem mora solicita reservas para a sua unidade e cancela as que solicitou; quem gerencia
 * o condomínio (administrador geral, síndico, administração e funcionário adm.) aprova ou rejeita. Quem vê e quem
 * altera cada reserva é decidido pelo {@link ReservaService}, a partir de quem está logado.
 */
@RestController
@RequestMapping("/api/v1/reservas")
@Tag(name = "Reservas")
public class ReservaApiController {

    private final ReservaService reservaService;
    private final PessoaService pessoaService;

    public ReservaApiController(ReservaService reservaService, PessoaService pessoaService) {
        this.reservaService = reservaService;
        this.pessoaService = pessoaService;
    }

    @GetMapping
    @Operation(summary = "Lista as reservas visíveis para quem está logado (as que solicitou e as dos condomínios "
            + "que gerencia), em ordem decrescente de data, como na tela")
    public Pagina<ReservaResposta> listar(@RequestParam(required = false) Integer condominioId,
                                          @RequestParam(required = false) ReservaStatus status,
                                          @RequestParam(required = false) String busca,
                                          @RequestParam(required = false) Integer areaId,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "20") int tamanho) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100),
                Sort.by(Sort.Direction.DESC, "data").and(Sort.by(Sort.Direction.DESC, "resCod")));
        return Pagina.de(reservaService.consultarReservas(usuarioLogado(), condominioId, status, busca, areaId,
                dataInicio, dataFim, pageable));
    }

    @GetMapping("/totais")
    @Operation(summary = "Quantidade de reservas: total, pendentes e aprovadas (só com o filtro de condomínio, "
            + "como no painel da tela)")
    public Map<String, Long> totais(@RequestParam(required = false) Integer condominioId) {
        return reservaService.contarReservas(usuarioLogado(), condominioId);
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Situações, condomínios, unidades e áreas para os filtros e o formulário de solicitação, "
            + "e se quem está logado pode aprovar ou solicitar reservas")
    public OpcoesReserva opcoes(@RequestParam(required = false) Integer condominioId) {
        return reservaService.opcoes(usuarioLogado(), condominioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Uma reserva, com os convidados")
    public ReservaResposta buscar(@PathVariable Integer id) {
        return reservaService.buscarPorIdDTO(id, usuarioLogado());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicita uma reserva para uma unidade de quem está logado, com os convidados e o aceite "
            + "dos termos de uso. A reserva fica pendente até a aprovação")
    public ReservaResposta solicitar(@Valid @RequestBody SolicitarReservaPedido pedido) {
        Pessoa usuario = usuarioLogado();
        Reserva reserva = reservaService.solicitarReserva(pedido.paraServico(), usuario);
        return reservaService.buscarPorIdDTO(reserva.getResCod(), usuario);
    }

    @PostMapping("/{id}/aprovar")
    @Operation(summary = "Aprova uma reserva pendente")
    public ReservaResposta aprovar(@PathVariable Integer id) {
        Pessoa usuario = usuarioLogado();
        reservaService.aprovarReserva(id, usuario);
        return reservaService.buscarPorIdDTO(id, usuario);
    }

    @PostMapping("/{id}/rejeitar")
    @Operation(summary = "Rejeita uma reserva pendente, com o motivo")
    public ReservaResposta rejeitar(@PathVariable Integer id, @Valid @RequestBody RejeitarReservaPedido pedido) {
        Pessoa usuario = usuarioLogado();
        reservaService.rejeitarReserva(id, usuario, pedido.motivo());
        return reservaService.buscarPorIdDTO(id, usuario);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancela uma reserva pendente ou aprovada que quem está logado solicitou")
    public ReservaResposta cancelar(@PathVariable Integer id) {
        Pessoa usuario = usuarioLogado();
        reservaService.cancelarReserva(id, usuario);
        return reservaService.buscarPorIdDTO(id, usuario);
    }

    private Pessoa usuarioLogado() {
        return pessoaService.getLoggedInUser();
    }
}
