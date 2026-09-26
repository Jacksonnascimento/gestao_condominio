package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.cliente.Cliente;
import br.com.gestaocondominio.api.cliente.ClienteAtual;
import br.com.gestaocondominio.api.cliente.ClientesAtendidos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reserva aprovada cuja data já passou vira concluída. Roda logo depois da meia-noite e também na subida da API, para
 * não depender de a API estar no ar na virada do dia. Um cliente de cada vez, cada um no seu banco: a falha num não
 * impede os outros.
 */
@Component
public class ConclusaoDeReservas {

    private static final Logger log = LoggerFactory.getLogger(ConclusaoDeReservas.class);

    private final ClientesAtendidos clientes;
    private final ReservaService reservaService;

    public ConclusaoDeReservas(ClientesAtendidos clientes, ReservaService reservaService) {
        this.clientes = clientes;
        this.reservaService = reservaService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${condigtal.reservas.conclusao:0 5 0 * * *}")
    public void concluirReservasPassadas() {
        if (!clientes.multiplos()) {
            concluir("instalação");
            return;
        }
        for (Cliente cliente : clientes.todos()) {
            ClienteAtual.executar(cliente, () -> concluir(cliente.identificador()));
        }
    }

    private void concluir(String onde) {
        try {
            int concluidas = reservaService.concluirReservasPassadas();
            if (concluidas > 0) {
                log.info("{} reserva(s) aprovada(s) que já passaram marcadas como concluídas ({}).", concluidas, onde);
            }
        } catch (RuntimeException e) {
            log.error("Não foi possível concluir as reservas que já passaram ({}).", onde, e);
        }
    }
}
