package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.UnidadeRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.service.UnidadeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }


    @GetMapping("/por-condominio/{condominioId}")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or hasAnyAuthority('ROLE_SINDICO_' + #condominioId, 'ROLE_ADMIN_' + #condominioId)")
    public ResponseEntity<List<Unidade>> listarUnidadesPorCondominio(@PathVariable Integer condominioId) {
        List<Unidade> unidades = unidadeService.findByCondominioId(condominioId);
        return new ResponseEntity<>(unidades, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or hasAnyAuthority('ROLE_SINDICO_' + #dto.conCod, 'ROLE_ADMIN_' + #dto.conCod)")
    public ResponseEntity<Unidade> cadastrarUnidade(@RequestBody UnidadeRequestDTO dto) {
        Unidade novaUnidade = unidadeService.cadastrarUnidade(dto);
        return new ResponseEntity<>(novaUnidade, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Unidade> buscarUnidadePorId(@PathVariable Integer id) {
        Optional<Unidade> unidade = unidadeService.buscarUnidadePorId(id);
        return unidade.map(u -> new ResponseEntity<>(u, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<Unidade>> listarTodasUnidades(
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativas,
            @RequestParam(required = false) String statusOcupacao,
            @RequestParam(required = false) String busca) {
        List<Unidade> unidades = unidadeService.listarTodasUnidades(incluirInativas, statusOcupacao, busca);
        return new ResponseEntity<>(unidades, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Unidade> atualizarUnidade(@PathVariable Integer id, @RequestBody UnidadeRequestDTO dto) {
        Unidade unidadeSalva = unidadeService.atualizarUnidade(id, dto);
        return new ResponseEntity<>(unidadeSalva, HttpStatus.OK);
    }

    @PutMapping("/{id}/inativar")
    public ResponseEntity<Unidade> inativarUnidade(@PathVariable Integer id) {
        Unidade unidadeInativada = unidadeService.inativarUnidade(id);
        return new ResponseEntity<>(unidadeInativada, HttpStatus.OK);
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Unidade> ativarUnidade(@PathVariable Integer id) {
        Unidade unidadeAtivada = unidadeService.ativarUnidade(id);
        return new ResponseEntity<>(unidadeAtivada, HttpStatus.OK);
    }
}