package br.com.gestaocondominio.api.controller.v1;

import br.com.gestaocondominio.api.cliente.ClienteAtual;
import br.com.gestaocondominio.api.controller.dto.UsuarioCondominioDTO;
import br.com.gestaocondominio.api.controller.v1.dto.AutenticacaoDTOs.Mensagem;
import br.com.gestaocondominio.api.controller.v1.dto.Opcao;
import br.com.gestaocondominio.api.controller.v1.dto.Pagina;
import br.com.gestaocondominio.api.controller.v1.dto.UsuarioDTOs.AcaoSenha;
import br.com.gestaocondominio.api.controller.v1.dto.UsuarioDTOs.CondominioDisponivel;
import br.com.gestaocondominio.api.controller.v1.dto.UsuarioDTOs.EditarUsuarioRequest;
import br.com.gestaocondominio.api.controller.v1.dto.UsuarioDTOs.NovoUsuarioRequest;
import br.com.gestaocondominio.api.controller.v1.dto.UsuarioDTOs.OcupanteSemLogin;
import br.com.gestaocondominio.api.controller.v1.dto.UsuarioDTOs.OpcoesUsuario;
import br.com.gestaocondominio.api.controller.v1.dto.UsuarioDTOs.PessoaCadastrada;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.service.OcupanteService;
import br.com.gestaocondominio.api.domain.service.PessoaService;
import br.com.gestaocondominio.api.domain.service.UsuarioCondominioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administração de usuários: quem acessa cada condomínio e com que papel. Um usuário é uma pessoa com um ou mais
 * vínculos (condomínio + papel); o vínculo é identificado por {@code pessoaId}, {@code condominioId} e {@code papel}.
 *
 * <p>Quem administra é o administrador geral, em todos os condomínios, e o síndico ou a administração, só nos
 * condomínios em que têm esse papel. As regras ficam no {@link UsuarioCondominioService}: ninguém cria ou altera
 * vínculo em condomínio que não administra, nem altera nome ou e-mail de quem tem acesso a outro condomínio, nem
 * remove ou muda o próprio acesso. Nenhuma rota daqui torna alguém administrador geral.</p>
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuários")
@PreAuthorize("hasAnyAuthority('ROLE_GLOBAL_ADMIN', 'ROLE_SINDICO', 'ROLE_ADMIN')")
public class UsuarioApiController {

    private static final List<UserRole> PAPEIS = List.of(UserRole.SINDICO, UserRole.ADMIN, UserRole.MORADOR,
            UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO);

    private final UsuarioCondominioService usuarioCondominioService;
    private final OcupanteService ocupanteService;
    private final PessoaService pessoaService;
    private final String webUrlPublica;

    public UsuarioApiController(UsuarioCondominioService usuarioCondominioService, OcupanteService ocupanteService,
                                PessoaService pessoaService,
                                @Value("${condigtal.web.url-publica:}") String webUrlPublica) {
        this.usuarioCondominioService = usuarioCondominioService;
        this.ocupanteService = ocupanteService;
        this.pessoaService = pessoaService;
        this.webUrlPublica = webUrlPublica;
    }

    @GetMapping
    @Operation(summary = "Lista os acessos ativos dos condomínios que quem está logado administra",
            description = "Sem condominioId, traz os de todos os condomínios administrados, por condomínio e nome.")
    public Pagina<UsuarioCondominioDTO> listar(@RequestParam(required = false) Integer condominioId,
                                               @RequestParam(defaultValue = "0") int pagina,
                                               @RequestParam(defaultValue = "20") int tamanho) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100));
        return Pagina.de(usuarioCondominioService.consultarVinculos(usuarioLogado(), condominioId, pageable));
    }

    @GetMapping("/opcoes")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Condomínios administrados, papéis e formas de definir a senha, e se quem está logado "
            + "administra usuários")
    public OpcoesUsuario opcoes() {
        Pessoa usuario = usuarioLogado();
        boolean podeGerenciar = usuarioCondominioService.podeGerenciarUsuarios(usuario);
        return new OpcoesUsuario(
                podeGerenciar
                        ? usuarioCondominioService.condominiosDisponiveis(usuario,
                                UsuarioCondominioService.PAPEIS_QUE_GERENCIAM_USUARIOS).stream()
                                .map(CondominioDisponivel::de).toList()
                        : List.of(),
                PAPEIS.stream().map(p -> new Opcao(p.name(), p.getDescricao())).toList(),
                Opcao.de(AcaoSenha.class, AcaoSenha::getDescricao),
                podeGerenciar);
    }

    @GetMapping("/ocupantes-sem-login")
    @Operation(summary = "Ocupantes de unidades do condomínio que ainda não têm acesso como morador")
    public List<OcupanteSemLogin> ocupantesSemLogin(@RequestParam Integer condominioId) {
        usuarioCondominioService.conferirGestaoDeUsuarios(usuarioLogado(), condominioId);
        return ocupanteService.findOcupantesDtoSemLoginMoradorByCondominio(condominioId).stream()
                .map(OcupanteSemLogin::de)
                .toList();
    }

    @GetMapping("/pessoa-por-cpf")
    @Operation(summary = "Pessoa já cadastrada com o CPF, para preencher o formulário de novo usuário")
    public PessoaCadastrada pessoaPorCpf(@RequestParam String cpf) {
        return PessoaCadastrada.de(usuarioCondominioService.buscarPessoaPorDocumento(usuarioLogado(), cpf));
    }

    @GetMapping("/{pessoaId}/vinculos/{condominioId}/{papel}")
    @Operation(summary = "Um acesso (pessoa, condomínio e papel)")
    public UsuarioCondominioDTO buscar(@PathVariable Integer pessoaId, @PathVariable Integer condominioId,
                                       @PathVariable UserRole papel) {
        return usuarioCondominioService.buscarVinculo(usuarioLogado(), pessoaId, condominioId, papel);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dá acesso a alguém num condomínio, cadastrando a pessoa se preciso",
            description = "Com ENVIAR_LINK (o padrão), a pessoa recebe por e-mail o link para definir a senha no "
                    + "sistema web, válido por 24 horas.")
    public UsuarioCondominioDTO cadastrar(@Valid @RequestBody NovoUsuarioRequest pedido, HttpServletRequest request) {
        return usuarioCondominioService.cadastrarUsuario(usuarioLogado(), pedido.paraDTO(),
                enderecoDoSistemaWeb(request));
    }

    @PutMapping("/{pessoaId}/vinculos/{condominioId}/{papel}")
    @Operation(summary = "Altera nome, e-mail e papel de um acesso",
            description = "Responde com o acesso já com o papel novo, que passa a ser o que identifica o vínculo.")
    public UsuarioCondominioDTO editar(@PathVariable Integer pessoaId, @PathVariable Integer condominioId,
                                       @PathVariable UserRole papel,
                                       @Valid @RequestBody EditarUsuarioRequest pedido) {
        return usuarioCondominioService.editarUsuario(usuarioLogado(), pessoaId, condominioId, papel,
                pedido.nome(), pedido.email(), pedido.papel());
    }

    @PostMapping("/{pessoaId}/link-de-senha")
    @Operation(summary = "Envia por e-mail o link para a pessoa definir uma nova senha no sistema web")
    public Mensagem enviarLinkDeSenha(@PathVariable Integer pessoaId, HttpServletRequest request) {
        String email = usuarioCondominioService.enviarLinkDeSenha(usuarioLogado(), pessoaId,
                enderecoDoSistemaWeb(request));
        return new Mensagem("Link de redefinição enviado para " + email + ".");
    }

    @DeleteMapping("/{pessoaId}/vinculos/{condominioId}/{papel}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um acesso (a pessoa continua cadastrada)")
    public void excluir(@PathVariable Integer pessoaId, @PathVariable Integer condominioId,
                        @PathVariable UserRole papel) {
        usuarioCondominioService.excluirVinculo(usuarioLogado(), pessoaId, condominioId, papel);
    }

    /**
     * Endereço do sistema web do cliente, onde o link do e-mail precisa abrir, como no
     * {@link AutenticacaoController}: vem de {@code WEB_URL_PUBLICA} (com {@code {cliente}} trocado pelo cliente
     * atual); sem ela, vale a origem de quem chamou.
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

    private Pessoa usuarioLogado() {
        return pessoaService.getLoggedInUser();
    }
}
