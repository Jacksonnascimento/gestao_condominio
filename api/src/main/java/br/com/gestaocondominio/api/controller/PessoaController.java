package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        return new ResponseEntity<>(novaPessoa, HttpStatus.CREATED); // 201 Created
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pessoa> buscarPessoaPorId(@PathVariable Integer id) {
        Optional<Pessoa> pessoa = pessoaService.buscarPessoaPorId(id);
        return pessoa.map(p -> new ResponseEntity<>(p, HttpStatus.OK)) // 200 OK
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); // 404 Not Found
    }

    @GetMapping
    public ResponseEntity<List<Pessoa>> listarTodasPessoas() {
        List<Pessoa> pessoas = pessoaService.listarTodasPessoas();
        return new ResponseEntity<>(pessoas, HttpStatus.OK); // 200 OK
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pessoa> atualizarPessoa(@PathVariable Integer id, @RequestBody Pessoa pessoaAtualizada) {
        Pessoa pessoaSalva = pessoaService.atualizarPessoa(id, pessoaAtualizada);
        return new ResponseEntity<>(pessoaSalva, HttpStatus.OK); // 200 OK
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativarPessoa(@PathVariable Integer id) {
    

        Pessoa pessoaParaDesativar = pessoaService.buscarPessoaPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Pessoa com ID " + id + " não encontrada para desativação."));

        pessoaParaDesativar.setPesAtivo(false); // Define como inativo
        pessoaService.atualizarPessoa(id, pessoaParaDesativar); // Persiste a mudança

        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content
    }
}