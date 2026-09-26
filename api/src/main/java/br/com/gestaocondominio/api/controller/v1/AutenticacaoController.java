package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.cliente.ClienteAtual;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.EsqueciSenhaRequest;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.LoginRequest;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.LoginResponse;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.Mensagem;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.RedefinirSenhaRequest;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.RenovarRequest;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.UsuarioLogado;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;
import br.com.gestaocondominio.api.domain.service.PasswordResetService;
import br.com.gestaocondominio.api.security.TokenService;
import br.com.gestaocondominio.api.security.TokenService.TokensEmitidos;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import br.com.gestaocondominio.api.security.UserDetailsServiceImpl;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login por token, renovação e redefinição de senha")
public class AutenticacaoController {

    private static final Logger log = LoggerFactory.getLogger(AutenticacaoController.class);
    private static final int VALIDADE_LINK_SENHA_HORAS = 1;

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserDetailsServiceImpl userDetailsService;
    private final PessoaRepository pessoaRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final PasswordResetService passwordResetService;
    private final String webUrlPublica;

    public AutenticacaoController(AuthenticationManager authenticationManager, TokenService tokenService,
                                  UserDetailsServiceImpl userDetailsService, PessoaRepository pessoaRepository,
                                  UsuarioCondominioRepository usuarioCondominioRepository,
                                  PasswordResetService passwordResetService,
                                  @Value("${condigtal.web.url-publica:}") String webUrlPublica) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
        this.pessoaRepository = pessoaRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.passwordResetService = passwordResetService;
        this.webUrlPublica = webUrlPublica;
    }

    @PostMapping("/login")
    @Operation(summary = "Entra com e-mail e senha e recebe os tokens")
    public LoginResponse login(@Valid @RequestBody LoginRequest pedido) {
        Authentication autenticacao;
        try {
            autenticacao = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(pedido.email().trim(), pedido.senha()));
        } catch (DisabledException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Este usuário está inativo.");
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Credenciais inválidas.", e);
        }
        return respostaDeLogin(((UserDetailsImpl) autenticacao.getPrincipal()).getPessoa());
    }

    @PostMapping("/renovar")
    @Operation(summary = "Troca o token de renovação por um par novo de tokens")
    public LoginResponse renovar(@Valid @RequestBody RenovarRequest pedido) {
        DecodedJWT token = tokenService.validarRenovacao(pedido.tokenRenovacao());
        if (token == null) {
            throw sessaoEncerrada();
        }
        UserDetailsImpl usuario;
        try {
            usuario = userDetailsService.loadUserByCodigo(Integer.valueOf(token.getSubject()));
        } catch (UsernameNotFoundException | NumberFormatException e) {
            throw sessaoEncerrada();
        }
        if (!usuario.isEnabled() || !tokenService.senhaConfere(token, usuario.getPessoa())) {
            throw sessaoEncerrada();
        }
        return respostaDeLogin(usuario.getPessoa());
    }

    @GetMapping("/eu")
    @Operation(summary = "Dados de quem está logado, com os condomínios e papéis")
    public UsuarioLogado eu(@AuthenticationPrincipal UserDetailsImpl usuario) {
        Pessoa pessoa = pessoaRepository.findById(usuario.getPessoa().getPesCod()).orElseThrow(this::sessaoEncerrada);
        return UsuarioLogado.de(pessoa, usuarioCondominioRepository.findByPessoa(pessoa));
    }

    /**
     * Responde sempre a mesma mensagem, exista ou não o e-mail: assim ninguém usa esta rota para descobrir quem tem
     * cadastro no sistema.
     */
    @PostMapping("/esqueci-senha")
    @Operation(summary = "Envia por e-mail o link para definir uma nova senha")
    public Mensagem esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest pedido, HttpServletRequest request) {
        try {
            passwordResetService.createPasswordResetToken(pedido.email().trim(), VALIDADE_LINK_SENHA_HORAS,
                    enderecoDoSistemaWeb(request));
        } catch (IllegalArgumentException e) {
            log.info("Pedido de redefinição de senha não atendido: {}", e.getMessage());
        }
        return new Mensagem("Se o e-mail estiver cadastrado, você receberá o link para definir uma nova senha.");
    }

    @GetMapping("/redefinir-senha/{token}")
    @Operation(summary = "Confere se o link de redefinição de senha ainda vale")
    public Mensagem conferirLinkDeSenha(@PathVariable String token) {
        passwordResetService.validatePasswordResetToken(token);
        return new Mensagem("Link válido.");
    }

    @PostMapping("/redefinir-senha")
    @Operation(summary = "Define a nova senha a partir do link recebido por e-mail")
    public Mensagem redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest pedido) {
        passwordResetService.resetPassword(pedido.token(), pedido.novaSenha());
        return new Mensagem("Senha definida. Entre com a nova senha.");
    }

    private LoginResponse respostaDeLogin(Pessoa pessoa) {
        TokensEmitidos tokens = tokenService.emitir(pessoa);
        return new LoginResponse(tokens.token(), tokens.tokenRenovacao(), tokens.expiraEm(),
                UsuarioLogado.de(pessoa, usuarioCondominioRepository.findByPessoa(pessoa)));
    }

    /**
     * Endereço do sistema web do cliente, que é onde o link do e-mail precisa abrir. Vem de {@code WEB_URL_PUBLICA}
     * (com {@code {cliente}} trocado pelo cliente atual); sem ela, vale a origem de quem chamou.
     */
    private String enderecoDoSistemaWeb(HttpServletRequest request) {
        String configurado = ClienteAtual.noEndereco(webUrlPublica);
        if (configurado != null && !configurado.isBlank()) {
            return configurado.replaceAll("/+$", "");
        }
        String origem = request.getHeader("Origin");
        if (origem != null && !origem.isBlank()) {
            return origem;
        }
        return request.getScheme() + "://" + request.getHeader("Host");
    }

    private ResponseStatusException sessaoEncerrada() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada ou inválida. Entre novamente.");
    }
}
