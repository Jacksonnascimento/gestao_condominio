package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.AdministradoraUsuario;
import br.com.gestaocondominio.api.domain.service.AdministradoraUsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/administradoras/usuarios") 
public class AdministradoraUsuarioController {

    private final AdministradoraUsuarioService administradoraUsuarioService;

    public AdministradoraUsuarioController(AdministradoraUsuarioService administradoraUsuarioService) {
        this.administradoraUsuarioService = administradoraUsuarioService;
    }

    @PostMapping
    public ResponseEntity<AdministradoraUsuario> cadastrarAdministradoraUsuario(@RequestBody AdministradoraUsuario administradoraUsuario) {
        AdministradoraUsuario novaAssociacao = administradoraUsuarioService.cadastrarAdministradoraUsuario(administradoraUsuario);
        return new ResponseEntity<>(novaAssociacao, HttpStatus.CREATED); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdministradoraUsuario> buscarAdministradoraUsuarioPorId(@PathVariable Integer id) {
        Optional<AdministradoraUsuario> associacao = administradoraUsuarioService.buscarAdministradoraUsuarioPorId(id);
        return associacao.map(au -> new ResponseEntity<>(au, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<AdministradoraUsuario>> listarTodosAdministradoraUsuarios(@RequestParam(required = false, defaultValue = "true") boolean ativos) {
       
        List<AdministradoraUsuario> usuarios = administradoraUsuarioService.listarTodosAdministradoraUsuarios(ativos);
        return new ResponseEntity<>(usuarios, HttpStatus.OK); 
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdministradoraUsuario> atualizarAdministradoraUsuario(@PathVariable Integer id, @RequestBody AdministradoraUsuario administradoraUsuarioAtualizado) {
        AdministradoraUsuario associacaoSalva = administradoraUsuarioService.atualizarAdministradoraUsuario(id, administradoraUsuarioAtualizado);
        return new ResponseEntity<>(associacaoSalva, HttpStatus.OK); 
    }

  
    @PutMapping("/{id}/inativar")
    public ResponseEntity<AdministradoraUsuario> inativarAdministradoraUsuario(@PathVariable Integer id) {
        AdministradoraUsuario associacaoInativada = administradoraUsuarioService.inativarAdministradoraUsuario(id);
        return new ResponseEntity<>(associacaoInativada, HttpStatus.OK);  
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<AdministradoraUsuario> ativarAdministradoraUsuario(@PathVariable Integer id) {
        AdministradoraUsuario associacaoAtivada = administradoraUsuarioService.ativarAdministradoraUsuario(id);
        return new ResponseEntity<>(associacaoAtivada, HttpStatus.OK);  
    }
}