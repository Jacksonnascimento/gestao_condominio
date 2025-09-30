package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.OcupanteRequestDTO;
import br.com.gestaocondominio.api.controller.dto.OcupanteResponseDTO;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.service.OcupanteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ocupantes")
public class OcupanteController {

    private final OcupanteService ocupanteService;

    public OcupanteController(OcupanteService ocupanteService) {
        this.ocupanteService = ocupanteService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OcupanteResponseDTO>> consultarOcupantes(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String vinculo,
            @RequestParam(required = false) Integer unidadeId) {
        
        List<Ocupante> ocupantes = ocupanteService.consultarOcupantes(busca, vinculo, unidadeId);
        List<OcupanteResponseDTO> dtos = ocupantes.stream()
                                                  .map(OcupanteResponseDTO::new)
                                                  .collect(Collectors.toList());
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or @ocupanteService.temPermissaoParaCriar(#dto.unidadeId)")
    public ResponseEntity<OcupanteResponseDTO> cadastrarOcupante(@RequestBody OcupanteRequestDTO dto) {
        Ocupante novoOcupante = ocupanteService.cadastrarOcupante(dto);
        return new ResponseEntity<>(new OcupanteResponseDTO(novoOcupante), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or @ocupanteService.temPermissaoParaGerenciar(#id)")
    public ResponseEntity<OcupanteResponseDTO> editarOcupante(@PathVariable Integer id, @RequestBody OcupanteRequestDTO dto) {
        Ocupante ocupanteAtualizado = ocupanteService.editarOcupante(id, dto);
        return new ResponseEntity<>(new OcupanteResponseDTO(ocupanteAtualizado), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or @ocupanteService.temPermissaoParaGerenciar(#id)")
    public ResponseEntity<Void> excluirOcupante(@PathVariable Integer id) {
        ocupanteService.excluirOcupante(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}