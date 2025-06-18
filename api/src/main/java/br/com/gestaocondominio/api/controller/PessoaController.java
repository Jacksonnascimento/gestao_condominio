package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.PessoaUpdateRequest; 
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pessoas")
public class PessoaController {

    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    @PostMapping
    public ResponseEntity<Pessoa> cadastrarPessoa(@RequestBody Pessoa pessoa) {
        Pessoa novaPessoa = pessoaService.cadastrarPessoa(pessoa);
        return new ResponseEntity<>(novaPessoa, HttpStatus.CREATED);
    }

    
    @PatchMapping("/{id}")
    public ResponseEntity<Pessoa> atualizarPessoa(@PathVariable Integer id, @RequestBody PessoaUpdateRequest dadosParaAtualizar) {
        Pessoa pessoaSalva = pessoaService.atualizarPessoa(id, dadosParaAtualizar);
        return new ResponseEntity<>(pessoaSalva, HttpStatus.OK);
    }
   
    @GetMapping("/{id}")
    public ResponseEntity<Pessoa> buscarPessoaPorId(@PathVariable Integer id) {
        Optional<Pessoa> pessoa = pessoaService.buscarPessoaPorId(id);
        return pessoa.map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<Pessoa>> listarTodasPessoas() {
        List<Pessoa> pessoas = pessoaService.listarTodasPessoas();
        return new ResponseEntity<>(pessoas, HttpStatus.OK);
    }

    @PutMapping("/{id}/inativar")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN')")
    public ResponseEntity<Pessoa> inativarPessoa(@PathVariable Integer id) {
        Pessoa pessoaInativada = pessoaService.inativarPessoa(id);
        return new ResponseEntity<>(pessoaInativada, HttpStatus.OK);
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<Pessoa> ativarPessoa(@PathVariable Integer id) {
        Pessoa pessoaAtivada = pessoaService.ativarPessoa(id);
        return new ResponseEntity<>(pessoaAtivada, HttpStatus.OK);
    }
}