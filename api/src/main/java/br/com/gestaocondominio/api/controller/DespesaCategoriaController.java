// src/main/java/br/com/gestaocondominio/api/controller/DespesaCategoriaController.java
package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.DespesaCategoria;
import br.com.gestaocondominio.api.domain.service.DespesaCategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/despesas/categorias")
public class DespesaCategoriaController {

    private final DespesaCategoriaService despesaCategoriaService;

    public DespesaCategoriaController(DespesaCategoriaService despesaCategoriaService) {
        this.despesaCategoriaService = despesaCategoriaService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or " +
                  "@despesaCategoriaService.hasPermissionToManageCategory(#categoria.condominio.conCod)") // CORRIGIDO AQUI
    public ResponseEntity<DespesaCategoria> cadastrarDespesaCategoria(@RequestBody DespesaCategoria categoria) {
        DespesaCategoria novaCategoria = despesaCategoriaService.cadastrarDespesaCategoria(categoria);
        return new ResponseEntity<>(novaCategoria, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DespesaCategoria> buscarDespesaCategoriaPorId(@PathVariable Integer id) {
        Optional<DespesaCategoria> categoria = despesaCategoriaService.buscarDespesaCategoriaPorId(id);
        return categoria.map(c -> new ResponseEntity<>(c, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<DespesaCategoria>> listarTodasDespesaCategorias(@RequestParam(required = false, defaultValue = "true") boolean ativas) {
        List<DespesaCategoria> categorias = despesaCategoriaService.listarTodasDespesaCategorias(ativas);
        return new ResponseEntity<>(categorias, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or " +
                  "@despesaCategoriaService.hasPermissionToManageCategory(#categoriaAtualizada.condominio.conCod)") // CORRIGIDO AQUI
    public ResponseEntity<DespesaCategoria> atualizarDespesaCategoria(@PathVariable Integer id, @RequestBody DespesaCategoria categoriaAtualizada) {
        DespesaCategoria categoriaSalva = despesaCategoriaService.atualizarDespesaCategoria(id, categoriaAtualizada);
        return new ResponseEntity<>(categoriaSalva, HttpStatus.OK);
    }

    @PutMapping("/{id}/inativar")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or " +
                  "@despesaCategoriaService.hasPermissionToManageCategory(#id)") // CORRIGIDO AQUI
    public ResponseEntity<DespesaCategoria> inativarDespesaCategoria(@PathVariable Integer id) {
        DespesaCategoria categoriaInativada = despesaCategoriaService.inativarDespesaCategoria(id);
        return new ResponseEntity<>(categoriaInativada, HttpStatus.OK);
    }

    @PutMapping("/{id}/ativar")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN') or " +
                  "@despesaCategoriaService.hasPermissionToManageCategory(#id)") // CORRIGIDO AQUI
    public ResponseEntity<DespesaCategoria> ativarDespesaCategoria(@PathVariable Integer id) {
        DespesaCategoria categoriaAtivada = despesaCategoriaService.ativarDespesaCategoria(id);
        return new ResponseEntity<>(categoriaAtivada, HttpStatus.OK);
    }
}