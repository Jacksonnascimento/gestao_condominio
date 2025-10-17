package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.OcupanteRequestDTO;
import br.com.gestaocondominio.api.controller.dto.OcupanteResponseDTO;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.service.OcupanteService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ocupantes")
public class OcupanteController {

    @Autowired
    private OcupanteService ocupanteService;

    @Autowired
    private PessoaService pessoaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OcupanteResponseDTO>> consultarOcupantes(
            @RequestParam(required = false) Integer condominioId,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) OcupanteVinculo vinculo) {
        
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        // ===== LINHA CORRIGIDA (agora recebe o DTO diretamente do serviço) =====
        List<OcupanteResponseDTO> dtos = ocupanteService.consultarOcupantesPorUsuario(usuarioLogado, condominioId, busca, vinculo);
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OcupanteResponseDTO> cadastrarOcupante(@RequestBody OcupanteRequestDTO dto) {
        Ocupante novoOcupante = ocupanteService.cadastrarOcupante(dto);
        return new ResponseEntity<>(new OcupanteResponseDTO(novoOcupante), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OcupanteResponseDTO> editarOcupante(@PathVariable Integer id, @RequestBody OcupanteRequestDTO dto) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        Ocupante ocupanteAtualizado = ocupanteService.editarOcupante(id, dto, usuarioLogado);
        return new ResponseEntity<>(new OcupanteResponseDTO(ocupanteAtualizado), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> excluirOcupante(@PathVariable Integer id) {
        Pessoa usuarioLogado = pessoaService.getLoggedInUser();
        ocupanteService.excluirOcupante(id, usuarioLogado);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}