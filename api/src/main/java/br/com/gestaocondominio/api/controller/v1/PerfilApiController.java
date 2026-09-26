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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/perfil")
@Tag(name = "Perfil", description = "Dados e senha de quem está logado")
public class PerfilApiController {

    private static final long TAMANHO_MAXIMO_DA_FOTO = 1024 * 1024;

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
        MediaType tipo = tipoDaImagem(imagem);
        return ResponseEntity.ok()
                .contentType(tipo == null ? MediaType.IMAGE_JPEG : tipo)
                .header("X-Content-Type-Options", "nosniff")
                .body(imagem);
    }

    /**
     * A foto fica no cadastro da pessoa e é lida junto com ele, por isso o limite é pequeno: o sistema web reduz a
     * imagem antes de enviar.
     */
    @PutMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Troca a foto de quem está logado",
            description = "Formulário multipart com a imagem na parte `foto`: JPEG, PNG ou WebP, de até 1 MB.")
    public UsuarioLogado trocarFoto(@AuthenticationPrincipal UserDetailsImpl usuario,
                                    @RequestPart("foto") MultipartFile foto) {
        if (foto.isEmpty()) {
            throw new IllegalArgumentException("A imagem escolhida está vazia.");
        }
        if (foto.getSize() > TAMANHO_MAXIMO_DA_FOTO) {
            throw new IllegalArgumentException("A foto pode ter até 1 MB.");
        }
        byte[] imagem;
        try {
            imagem = foto.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler a imagem enviada.");
        }
        // Pelo conteúdo, e não pelo nome ou pelo tipo informado, que qualquer um escolhe
        if (tipoDaImagem(imagem) == null) {
            throw new IllegalArgumentException("Envie a foto em JPEG, PNG ou WebP.");
        }
        Pessoa pessoa = pessoaService.atualizarFoto(usuario.getPessoa().getPesCod(), imagem);
        return UsuarioLogado.de(pessoa, usuarioCondominioRepository.findByPessoa(pessoa));
    }

    @DeleteMapping("/foto")
    @Operation(summary = "Tira a foto de quem está logado")
    public UsuarioLogado tirarFoto(@AuthenticationPrincipal UserDetailsImpl usuario) {
        Pessoa pessoa = pessoaService.atualizarFoto(usuario.getPessoa().getPesCod(), null);
        return UsuarioLogado.de(pessoa, usuarioCondominioRepository.findByPessoa(pessoa));
    }

    /** Tipo da imagem pelos primeiros bytes: JPEG, PNG ou WebP. Nulo para qualquer outra coisa. */
    static MediaType tipoDaImagem(byte[] dados) {
        if (dados.length >= 3 && (dados[0] & 0xFF) == 0xFF && (dados[1] & 0xFF) == 0xD8 && (dados[2] & 0xFF) == 0xFF) {
            return MediaType.IMAGE_JPEG;
        }
        if (dados.length >= 8 && (dados[0] & 0xFF) == 0x89 && dados[1] == 'P' && dados[2] == 'N' && dados[3] == 'G'
                && dados[4] == 0x0D && dados[5] == 0x0A && dados[6] == 0x1A && dados[7] == 0x0A) {
            return MediaType.IMAGE_PNG;
        }
        if (dados.length >= 12 && dados[0] == 'R' && dados[1] == 'I' && dados[2] == 'F' && dados[3] == 'F'
                && dados[8] == 'W' && dados[9] == 'E' && dados[10] == 'B' && dados[11] == 'P') {
            return MediaType.parseMediaType("image/webp");
        }
        return null;
    }
}
