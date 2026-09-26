package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.controller.dto.PessoaUpdateRequest;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.AtualizarPerfilRequest;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.Mensagem;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.TrocarSenhaRequest;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.UsuarioLogado;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/perfil")
@Tag(name = "Perfil", description = "Dados e senha de quem está logado")
public class PerfilApiController {

    private final PessoaService pessoaService;
    private final UsuarioCondominioRepository usuarioCondominioRepository;

    public PerfilApiController(PessoaService pessoaService, UsuarioCondominioRepository usuarioCondominioRepository) {
        this.pessoaService = pessoaService;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
    }

    @PatchMapping
    @Operation(summary = "Altera nome e telefones")
    public UsuarioLogado atualizar(@AuthenticationPrincipal UserDetailsImpl usuario,
                                   @Valid @RequestBody AtualizarPerfilRequest pedido) {
        Pessoa pessoa = pessoaService.atualizarPessoa(usuario.getPessoa().getPesCod(), new PessoaUpdateRequest(
                pedido.nome().trim(), null, null, null, pedido.telefone(), pedido.telefone2(), null, null));
        return UsuarioLogado.de(pessoa, usuarioCondominioRepository.findByPessoa(pessoa));
    }

    /** Trocar a senha derruba os tokens já emitidos; quem trocou precisa entrar de novo nos outros aparelhos. */
    @PutMapping("/senha")
    @Operation(summary = "Troca a senha, conferindo a atual")
    public Mensagem trocarSenha(@AuthenticationPrincipal UserDetailsImpl usuario,
                                @Valid @RequestBody TrocarSenhaRequest pedido) {
        try {
            pessoaService.atualizarSenha(usuario.getPessoa().getPesCod(), pedido.senhaAtual(), pedido.novaSenha());
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha atual informada está incorreta.");
        }
        return new Mensagem("Senha alterada. Entre novamente com a nova senha.");
    }

    @GetMapping("/foto")
    @Operation(summary = "Foto de quem está logado")
    public ResponseEntity<byte[]> foto(@AuthenticationPrincipal UserDetailsImpl usuario) {
        byte[] imagem = usuario.getPessoa().getPesImagem();
        if (imagem == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(imagem);
    }
}
