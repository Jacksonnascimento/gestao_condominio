package br.com.gestaocondominio.api.controller;

import br.com.gestaocondominio.api.controller.dto.PessoaUpdateRequest;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.service.PessoaService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    // Usada pelos formulários de ocupante e de usuário, que só a gestão abre.
    @GetMapping("/por-cpf/{cpfCnpj}")
    @PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_FUNCIONARIO_ADM')")
    public ResponseEntity<Pessoa> buscarPessoaPorCpfCnpj(@PathVariable String cpfCnpj) {
        return pessoaService.buscarPorCpfCnpj(cpfCnpj)
                .map(pessoa -> new ResponseEntity<>(pessoa, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN')")
    @PostMapping
    public ResponseEntity<Pessoa> cadastrarPessoa(@RequestBody Pessoa pessoa) {
        Pessoa novaPessoa = pessoaService.cadastrarPessoa(pessoa);
        return new ResponseEntity<>(novaPessoa, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("#id.equals(authentication.principal.pessoa.pesCod) or hasAuthority('ROLE_GLOBAL_ADMIN')")
    public ResponseEntity<Pessoa> atualizarPessoa(@PathVariable Integer id,
            @RequestBody PessoaUpdateRequest dadosParaAtualizar) {
        Pessoa pessoaSalva = pessoaService.atualizarPessoa(id, dadosParaAtualizar);
        return new ResponseEntity<>(pessoaSalva, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Pessoa> buscarPessoaPorId(@PathVariable Integer id) {

        Optional<Pessoa> pessoa = pessoaService.buscarPessoaPorId(id);
        return pessoa.map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN')")
    @GetMapping
    public ResponseEntity<List<Pessoa>> listarTodasPessoas() {

        List<Pessoa> pessoas = pessoaService.listarPessoasAutorizadas();
        return new ResponseEntity<>(pessoas, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN')")
    @GetMapping("/{id}/imagem")
    public ResponseEntity<byte[]> buscarImagemDaPessoa(@PathVariable Integer id) {
        byte[] imagem = pessoaService.buscarImagemPorId(id);
        if (imagem != null && imagem.length > 0) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            return new ResponseEntity<>(imagem, headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}/inativar")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN')")
    public ResponseEntity<Pessoa> inativarPessoa(@PathVariable Integer id) {
        Pessoa pessoaInativada = pessoaService.inativarPessoa(id);
        return new ResponseEntity<>(pessoaInativada, HttpStatus.OK);
    }

    @PutMapping("/{id}/ativar")
    @PreAuthorize("hasAuthority('ROLE_GLOBAL_ADMIN')")
    public ResponseEntity<Pessoa> ativarPessoa(@PathVariable Integer id) {
        Pessoa pessoaAtivada = pessoaService.ativarPessoa(id);
        return new ResponseEntity<>(pessoaAtivada, HttpStatus.OK);
    }
}