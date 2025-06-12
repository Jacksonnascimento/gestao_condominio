package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominioId; 
import br.com.gestaocondominio.api.domain.enums.UserRole; 
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios/condominios") 
public class UsuarioCondominioController {

    private final UsuarioCondominioService usuarioCondominioService;

    public UsuarioCondominioController(UsuarioCondominioService usuarioCondominioService) {
        this.usuarioCondominioService = usuarioCondominioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioCondominio> cadastrarUsuarioCondominio(@RequestBody UsuarioCondominio usuarioCondominio) {
        UsuarioCondominio novoUsuarioCondominio = usuarioCondominioService.cadastrarUsuarioCondominio(usuarioCondominio);
        return new ResponseEntity<>(novoUsuarioCondominio, HttpStatus.CREATED); 
    }

  
    @GetMapping("/{pesCod}/{conCod}/{papel}")
    public ResponseEntity<UsuarioCondominio> buscarUsuarioCondominioPorId(@PathVariable Integer pesCod, @PathVariable Integer conCod, @PathVariable UserRole papel) { 
        UsuarioCondominioId id = new UsuarioCondominioId(pesCod, conCod, papel); 
        Optional<UsuarioCondominio> usuarioCondominio = usuarioCondominioService.buscarUsuarioCondominioPorId(id);
        return usuarioCondominio.map(uc -> new ResponseEntity<>(uc, HttpStatus.OK)) 
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); 
    }

    @GetMapping
    public ResponseEntity<List<UsuarioCondominio>> listarTodosUsuariosCondominio(@RequestParam(required = false, defaultValue = "true") boolean ativos) {
        List<UsuarioCondominio> usuariosCondominio = usuarioCondominioService.listarTodosUsuariosCondominio(ativos);
        return new ResponseEntity<>(usuariosCondominio, HttpStatus.OK); 
    }

    
    @PutMapping("/{pesCod}/{conCod}/{papel}")
    public ResponseEntity<UsuarioCondominio> atualizarUsuarioCondominio(@PathVariable Integer pesCod, @PathVariable Integer conCod, @PathVariable UserRole papel, @RequestBody UsuarioCondominio usuarioCondominioAtualizado) { 
        UsuarioCondominioId id = new UsuarioCondominioId(pesCod, conCod, papel); 
        UsuarioCondominio usuarioCondominioSalvo = usuarioCondominioService.atualizarUsuarioCondominio(id, usuarioCondominioAtualizado);
        return new ResponseEntity<>(usuarioCondominioSalvo, HttpStatus.OK); 
    }

    @PutMapping("/{pesCod}/{conCod}/{papel}/inativar") 
    public ResponseEntity<UsuarioCondominio> inativarUsuarioCondominio(@PathVariable Integer pesCod, @PathVariable Integer conCod, @PathVariable UserRole papel) { 
        UsuarioCondominioId id = new UsuarioCondominioId(pesCod, conCod, papel); 
        UsuarioCondominio usuarioCondominioInativado = usuarioCondominioService.inativarUsuarioCondominio(id);
        return new ResponseEntity<>(usuarioCondominioInativado, HttpStatus.OK); 
    }

    @PutMapping("/{pesCod}/{conCod}/{papel}/ativar") 
    public ResponseEntity<UsuarioCondominio> ativarUsuarioCondominio(@PathVariable Integer pesCod, @PathVariable Integer conCod, @PathVariable UserRole papel) { 
        UsuarioCondominioId id = new UsuarioCondominioId(pesCod, conCod, papel); 
        UsuarioCondominio usuarioCondominioAtivado = usuarioCondominioService.ativarUsuarioCondominio(id);
        return new ResponseEntity<>(usuarioCondominioAtivado, HttpStatus.OK); 
    }
}