package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.ReservaAreaComum;
import br.com.gestaocondominio.api.domain.service.ReservaAreaComumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reservas")
public class ReservaAreaComumController {

    private final ReservaAreaComumService reservaAreaComumService;

    public ReservaAreaComumController(ReservaAreaComumService reservaAreaComumService) {
        this.reservaAreaComumService = reservaAreaComumService;
    }

    @PostMapping
    public ResponseEntity<ReservaAreaComum> cadastrarReservaAreaComum(@RequestBody ReservaAreaComum reserva) {
        ReservaAreaComum novaReserva = reservaAreaComumService.cadastrarReservaAreaComum(reserva);
        return new ResponseEntity<>(novaReserva, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaAreaComum> buscarReservaAreaComumPorId(@PathVariable Integer id) {
        Optional<ReservaAreaComum> reserva = reservaAreaComumService.buscarReservaAreaComumPorId(id);
        return reserva.map(r -> new ResponseEntity<>(r, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<ReservaAreaComum>> listarTodasReservasAreaComum() {
        List<ReservaAreaComum> reservas = reservaAreaComumService.listarTodasReservasAreaComum();
        return new ResponseEntity<>(reservas, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservaAreaComum> atualizarReservaAreaComum(@PathVariable Integer id, @RequestBody ReservaAreaComum reservaAtualizada) {
        ReservaAreaComum reservaSalva = reservaAreaComumService.atualizarReservaAreaComum(id, reservaAtualizada);
        return new ResponseEntity<>(reservaSalva, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarReservaAreaComum(@PathVariable Integer id) {
        reservaAreaComumService.deletarReservaAreaComum(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}