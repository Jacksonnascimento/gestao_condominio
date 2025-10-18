package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.ContratoRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Contrato;
import br.com.gestaocondominio.api.domain.enums.StatusContrato;
import br.com.gestaocondominio.api.domain.service.ContratoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/condominios/{condominioId}/contratos")
public class ContratoController {

    @Autowired
    private ContratoService contratoService;
    
    @PostMapping
    public ResponseEntity<Contrato> criarContrato(@PathVariable Integer condominioId, @RequestBody ContratoRequestDTO dto) {
        return ResponseEntity.ok(contratoService.criarContrato(condominioId, dto));
    }

    @GetMapping
    public ResponseEntity<List<Contrato>> listarContratos(
            @PathVariable Integer condominioId,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) StatusContrato status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(contratoService.listarContratos(condominioId, busca, status, null, null, dataInicio, dataFim));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contrato> obterContratoPorId(@PathVariable Long id) {
        Contrato contrato = contratoService.obterPorId(id);
        return contrato != null ? ResponseEntity.ok(contrato) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contrato> atualizarContrato(@PathVariable Long id, @RequestBody ContratoRequestDTO dto) {
        return ResponseEntity.ok(contratoService.atualizarContrato(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarContrato(@PathVariable Long id) {
        contratoService.deletarContrato(id);
        return ResponseEntity.noContent().build();
    }
}