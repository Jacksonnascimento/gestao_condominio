package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.PessoaUpdateRequest;
import br.com.gestaocondominio.api.controller.dto.UsuarioCondominioDTO;
import br.com.gestaocondominio.api.controller.dto.UsuarioCondominioRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominioId;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;
import br.com.gestaocondominio.api.exception.ConflitoException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioCondominioService {

    /** Papéis que administram os usuários do condomínio (tela "Admin. Usuários"). */
    public static final UserRole[] PAPEIS_QUE_GERENCIAM_USUARIOS = {UserRole.SINDICO, UserRole.ADMIN};

    /** Validade, em horas, do link de definição de senha mandado pela administração de usuários. */
    private static final int VALIDADE_LINK_SENHA_HORAS = 24;

    public static final String ACAO_SENHA_ENVIAR_LINK = "ENVIAR_LINK";
    public static final String ACAO_SENHA_CRIAR_SENHA = "CRIAR_SENHA";

    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final PessoaRepository pessoaRepository;
    private final CondominioRepository condominioRepository;
    private final OcupanteRepository ocupanteRepository;
    private final PessoaService pessoaService;
    private final PasswordResetService passwordResetService;

    public UsuarioCondominioService(UsuarioCondominioRepository usuarioCondominioRepository,
            PessoaRepository pessoaRepository,
            CondominioRepository condominioRepository,
            OcupanteRepository ocupanteRepository,
            PessoaService pessoaService,
            PasswordResetService passwordResetService) {
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.pessoaRepository = pessoaRepository;
        this.condominioRepository = condominioRepository;
        this.ocupanteRepository = ocupanteRepository;
        this.pessoaService = pessoaService;
        this.passwordResetService = passwordResetService;
    }

    /** Grava o vínculo da pessoa com o condomínio, no papel informado. */
    private UsuarioCondominio cadastrarUsuarioCondominio(UsuarioCondominio usuarioCondominio) {

        if (usuarioCondominio.getPessoa() == null || usuarioCondominio.getPessoa().getPesCod() == null) {
            throw new IllegalArgumentException("Informe a pessoa.");
        }
        Pessoa pessoa = pessoaRepository.findById(usuarioCondominio.getPessoa().getPesCod())
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));
        usuarioCondominio.setPessoa(pessoa);

        if (usuarioCondominio.getCondominio() == null || usuarioCondominio.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Escolha o condomínio.");
        }
        Condominio condominio = condominioRepository.findById(usuarioCondominio.getCondominio().getConCod())
                .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado."));
        usuarioCondominio.setCondominio(condominio);

        if (usuarioCondominio.getUscPapel() == null) {
            throw new IllegalArgumentException("Informe o papel do usuário.");
        }

        usuarioCondominio.setPesCod(pessoa.getPesCod());
        usuarioCondominio.setConCod(condominio.getConCod());

        UsuarioCondominioId idComposto = new UsuarioCondominioId(
                usuarioCondominio.getPesCod(),
                usuarioCondominio.getConCod(),
                usuarioCondominio.getUscPapel());
        Optional<UsuarioCondominio> existente = usuarioCondominioRepository.findById(idComposto);
        if (existente.isPresent()) {
            throw new ConflitoException(Boolean.TRUE.equals(existente.get().getUscAtivoAssociacao())
                    ? "Esta pessoa já possui este papel neste condomínio."
                    : "Esta pessoa já tem este papel neste condomínio, mas o acesso está desativado. Reative-o na "
                            + "lista de usuários.");
        }

        if (usuarioCondominio.getUscAtivoAssociacao() == null) {
            usuarioCondominio.setUscAtivoAssociacao(true);
        }
        usuarioCondominio.setUscDtAssociacao(LocalDateTime.now());
        usuarioCondominio.setUscDtAtualizacao(LocalDateTime.now());

        return usuarioCondominioRepository.save(usuarioCondominio);
    }

    /**
     * Troca o papel do vínculo. O papel faz parte da chave, então o registro antigo sai e entra um novo, mas o acesso
     * é o mesmo: a data em que ele foi dado e a situação (ativo ou desativado) continuam as de antes.
     */
    private UsuarioCondominio atualizarPapelUsuario(Integer pessoaId, Integer condominioId, UserRole oldPapel, UserRole newPapel) {
        if (oldPapel == newPapel) {
             throw new IllegalArgumentException("O novo papel deve ser diferente do papel atual.");
        }

        UsuarioCondominio oldVinculo = vinculoExistente(pessoaId, condominioId, oldPapel);
        if (usuarioCondominioRepository.existsById(new UsuarioCondominioId(pessoaId, condominioId, newPapel))) {
            throw new ConflitoException("Esta pessoa já tem o papel " + newPapel.getDescricao()
                    + " neste condomínio.");
        }

        UsuarioCondominio novoVinculo = new UsuarioCondominio();
        novoVinculo.setPesCod(pessoaId);
        novoVinculo.setConCod(condominioId);
        novoVinculo.setPessoa(oldVinculo.getPessoa());
        novoVinculo.setCondominio(oldVinculo.getCondominio());
        novoVinculo.setUscPapel(newPapel);
        novoVinculo.setUscDtAssociacao(oldVinculo.getUscDtAssociacao());
        novoVinculo.setUscAtivoAssociacao(oldVinculo.getUscAtivoAssociacao());
        novoVinculo.setUscDtAtualizacao(LocalDateTime.now());

        usuarioCondominioRepository.delete(oldVinculo);
        return usuarioCondominioRepository.save(novoVinculo);
    }
    
    public List<UsuarioCondominio> findByPessoa(Pessoa pessoa) {
        return usuarioCondominioRepository.findByPesCod(pessoa.getPesCod());
    }

    /** Se a pessoa tem, em algum condomínio, um vínculo ativo com um dos papéis. Vínculo desativado não conta. */
    public boolean possuiRole(Pessoa pessoa, UserRole... roles) {
        if (pessoa == null || roles == null) {
            return false;
        }
        List<UsuarioCondominio> associacoes = findByPessoa(pessoa);
        for (UsuarioCondominio assoc : associacoes) {
            if (!Boolean.TRUE.equals(assoc.getUscAtivoAssociacao())) {
                continue;
            }
            for (UserRole role : roles) {
                if (assoc.getUscPapel() == role) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Condomínio de um dos vínculos ativos da pessoa. Quem tem vários condomínios recebe só um deles: os comunicados,
     * as encomendas e as ocorrências ainda trabalham com um condomínio por pessoa; o resto usa o alcance por
     * condomínio, logo abaixo.
     */
    public Integer getCondominioIdDoUsuario(Pessoa pessoa) {
        return findByPessoa(pessoa).stream()
                .filter(vinculo -> Boolean.TRUE.equals(vinculo.getUscAtivoAssociacao()))
                .findFirst()
                .map(UsuarioCondominio::getConCod)
                .orElse(null);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Alcance por condomínio. Usado pela API v1 para decidir o que cada pessoa vê e altera em cada condomínio.
    // ---------------------------------------------------------------------------------------------------------------

    public static boolean isAdministradorGeral(Pessoa pessoa) {
        return pessoa != null && Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin());
    }

    /**
     * Códigos dos condomínios em que a pessoa tem vínculo ativo com um dos papéis. Não trata o administrador geral,
     * que alcança todos: quem chama decide o que fazer com ele.
     */
    public Set<Integer> condominiosComPapel(Pessoa pessoa, UserRole... papeis) {
        List<UserRole> procurados = Arrays.asList(papeis);
        return findByPessoa(pessoa).stream()
                .filter(vinculo -> Boolean.TRUE.equals(vinculo.getUscAtivoAssociacao()))
                .filter(vinculo -> procurados.contains(vinculo.getUscPapel()))
                .map(UsuarioCondominio::getConCod)
                .collect(Collectors.toSet());
    }

    /** Se a pessoa tem um dos papéis, ativo, no condomínio. O administrador geral sempre tem. */
    public boolean possuiPapelNoCondominio(Pessoa pessoa, Integer condominioId, UserRole... papeis) {
        return isAdministradorGeral(pessoa)
                || (condominioId != null && condominiosComPapel(pessoa, papeis).contains(condominioId));
    }

    /**
     * Condomínios que a pessoa alcança com um dos papéis, em ordem de nome. Para o administrador geral, todos os
     * condomínios cadastrados.
     */
    @Transactional(readOnly = true)
    public List<Condominio> condominiosDisponiveis(Pessoa pessoa, UserRole... papeis) {
        List<Condominio> condominios = isAdministradorGeral(pessoa)
                ? condominioRepository.findAll()
                : condominioRepository.findAllById(condominiosComPapel(pessoa, papeis));
        return condominios.stream()
                .sorted(Comparator.comparing(Condominio::getConNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Administração de usuários (API v1). Quem administra é o administrador geral, em todos os condomínios, e o
    // síndico ou a administração, só nos condomínios em que têm esse papel. Nenhuma destas operações torna alguém
    // administrador geral.
    // ---------------------------------------------------------------------------------------------------------------

    public boolean podeGerenciarUsuarios(Pessoa usuario) {
        return isAdministradorGeral(usuario) || !condominiosComPapel(usuario, PAPEIS_QUE_GERENCIAM_USUARIOS).isEmpty();
    }

    /** Barra quem não administra os usuários do condomínio. Vem antes de qualquer busca, para não revelar registros. */
    public void conferirGestaoDeUsuarios(Pessoa usuario, Integer condominioId) {
        if (!possuiPapelNoCondominio(usuario, condominioId, PAPEIS_QUE_GERENCIAM_USUARIOS)) {
            throw new AccessDeniedException("Você não administra os usuários deste condomínio.");
        }
    }

    /**
     * Vínculos dos condomínios que o usuário administra, por condomínio, nome e papel. Com {@code condominioId}, só os
     * daquele condomínio; com {@code ativo}, só os ativos ({@code true}) ou só os desativados ({@code false}).
     */
    @Transactional(readOnly = true)
    public Page<UsuarioCondominioDTO> consultarVinculos(Pessoa usuario, Integer condominioId, Boolean ativo,
                                                        Pageable pageable) {
        List<UsuarioCondominioDTO> vinculos = vinculosAlcancados(usuario, condominioId).stream()
                .filter(vinculo -> ativo == null || ativo.equals(vinculo.getUscAtivoAssociacao()))
                .sorted(Comparator
                        .comparing((UsuarioCondominio v) -> v.getCondominio().getConNome(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(v -> v.getPessoa().getPesNome(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(UsuarioCondominio::getUscPapel))
                .map(UsuarioCondominioDTO::new)
                .toList();

        int inicio = (int) Math.min(pageable.getOffset(), vinculos.size());
        int fim = Math.min(inicio + pageable.getPageSize(), vinculos.size());
        return new PageImpl<>(vinculos.subList(inicio, fim), pageable, vinculos.size());
    }

    /** Quantidade de vínculos ativos ({@code ATIVOS}), desativados ({@code DESATIVADOS}) e no total ({@code TOTAL}). */
    @Transactional(readOnly = true)
    public Map<String, Long> contarVinculos(Pessoa usuario, Integer condominioId) {
        List<UsuarioCondominio> vinculos = vinculosAlcancados(usuario, condominioId);
        long ativos = vinculos.stream().filter(v -> Boolean.TRUE.equals(v.getUscAtivoAssociacao())).count();
        return Map.of("TOTAL", (long) vinculos.size(), "ATIVOS", ativos, "DESATIVADOS", vinculos.size() - ativos);
    }

    /** Vínculos, ativos e desativados, dos condomínios que o usuário administra (ou só do {@code condominioId}). */
    private List<UsuarioCondominio> vinculosAlcancados(Pessoa usuario, Integer condominioId) {
        Set<Integer> alcance;
        if (condominioId != null) {
            conferirGestaoDeUsuarios(usuario, condominioId);
            alcance = Set.of(condominioId);
        } else if (isAdministradorGeral(usuario)) {
            alcance = null;
        } else {
            alcance = condominiosComPapel(usuario, PAPEIS_QUE_GERENCIAM_USUARIOS);
            if (alcance.isEmpty()) {
                throw new AccessDeniedException("Você não administra os usuários de nenhum condomínio.");
            }
        }

        List<UsuarioCondominio> vinculos = alcance != null && alcance.size() == 1
                ? usuarioCondominioRepository.findByConCod(alcance.iterator().next())
                : usuarioCondominioRepository.findAll();
        return vinculos.stream()
                .filter(vinculo -> alcance == null || alcance.contains(vinculo.getConCod()))
                .filter(vinculo -> vinculo.getPessoa() != null && vinculo.getCondominio() != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioCondominioDTO buscarVinculo(Pessoa usuario, Integer pessoaId, Integer condominioId, UserRole papel) {
        conferirGestaoDeUsuarios(usuario, condominioId);
        return new UsuarioCondominioDTO(vinculoExistente(pessoaId, condominioId, papel));
    }

    /**
     * Pessoa já cadastrada com o CPF/CNPJ, para a tela preencher os dados ao criar o acesso. Só para quem administra
     * usuários.
     */
    @Transactional(readOnly = true)
    public Pessoa buscarPessoaPorDocumento(Pessoa usuario, String cpfCnpj) {
        if (!podeGerenciarUsuarios(usuario)) {
            throw new AccessDeniedException("Você não administra usuários.");
        }
        return pessoaPorDocumento(cpfCnpj)
                .orElseThrow(() -> new EntityNotFoundException("Nenhuma pessoa cadastrada com este CPF."));
    }

    /**
     * Dá acesso a alguém num condomínio, como na tela "Novo Usuário":
     * <ul>
     *   <li>com {@code pessoaId}, a pessoa é um ocupante de unidade do condomínio (obrigatório para o papel Morador);</li>
     *   <li>sem ele, vale o CPF: se já houver cadastro com esse CPF, o vínculo é feito com essa pessoa e os demais dados
     *   pessoais são ignorados; se não houver, a pessoa é cadastrada com os dados informados.</li>
     * </ul>
     * A senha é definida na hora ({@code CRIAR_SENHA}, só para pessoa nova) ou pelo link enviado por e-mail
     * ({@code ENVIAR_LINK}, o padrão), que abre em {@code enderecoWeb}.
     */
    @Transactional
    public UsuarioCondominioDTO cadastrarUsuario(Pessoa usuario, UsuarioCondominioRequestDTO dados, String enderecoWeb) {
        if (dados.getPapel() == null) {
            throw new IllegalArgumentException("Informe o papel do usuário.");
        }
        Integer condominioId = dados.getCondominioId() != null
                ? dados.getCondominioId()
                : unicoCondominioAdministrado(usuario);
        conferirGestaoDeUsuarios(usuario, condominioId);
        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado."));

        String acaoSenha = StringUtils.hasText(dados.getAcaoSenha()) ? dados.getAcaoSenha() : ACAO_SENHA_ENVIAR_LINK;
        if (!ACAO_SENHA_ENVIAR_LINK.equals(acaoSenha) && !ACAO_SENHA_CRIAR_SENHA.equals(acaoSenha)) {
            throw new IllegalArgumentException("Escolha como a senha será definida: ENVIAR_LINK ou CRIAR_SENHA.");
        }
        boolean criarSenha = ACAO_SENHA_CRIAR_SENHA.equals(acaoSenha);

        Pessoa pessoa;
        if (dados.getPessoaId() != null) {
            pessoa = ocupanteDoCondominio(dados.getPessoaId(), condominioId);
            if (criarSenha) {
                throw new IllegalArgumentException(
                        "A senha de um ocupante já cadastrado é definida pelo link enviado por e-mail.");
            }
        } else if (dados.getPapel() == UserRole.MORADOR) {
            throw new IllegalArgumentException("Para o papel Morador, escolha um ocupante de unidade do condomínio.");
        } else {
            Optional<Pessoa> existente = pessoaPorDocumento(dados.getPesCpfCnpj());
            if (existente.isPresent()) {
                pessoa = existente.get();
                if (criarSenha) {
                    throw new IllegalArgumentException("Esta pessoa já tem cadastro; a senha dela só pode ser definida "
                            + "pelo link enviado por e-mail.");
                }
            } else {
                pessoa = cadastrarPessoaNova(dados, criarSenha);
            }
        }

        UsuarioCondominio vinculo = new UsuarioCondominio();
        vinculo.setPessoa(pessoa);
        vinculo.setCondominio(condominio);
        vinculo.setUscPapel(dados.getPapel());
        UsuarioCondominio salvo = cadastrarUsuarioCondominio(vinculo);

        if (!criarSenha) {
            passwordResetService.createPasswordResetToken(pessoa.getPesEmail(), VALIDADE_LINK_SENHA_HORAS, enderecoWeb);
        }
        return new UsuarioCondominioDTO(salvo);
    }

    /**
     * Altera nome, e-mail e papel de um vínculo, como na tela "Editar Usuário". Nome e e-mail são da pessoa e valem em
     * todos os condomínios dela: por isso, quem não é administrador geral só os altera se a pessoa não for
     * administrador geral nem tiver acesso a condomínio que ele não administre. Ninguém muda o próprio papel.
     */
    @Transactional
    public UsuarioCondominioDTO editarUsuario(Pessoa usuario, Integer pessoaId, Integer condominioId,
                                              UserRole papelAtual, String nome, String email, UserRole novoPapel) {
        conferirGestaoDeUsuarios(usuario, condominioId);
        UsuarioCondominio vinculo = vinculoExistente(pessoaId, condominioId, papelAtual);
        Pessoa pessoa = vinculo.getPessoa();

        String nomeNovo = StringUtils.hasText(nome) ? nome.trim() : null;
        String emailNovo = StringUtils.hasText(email) ? email.trim() : null;
        boolean nomeMudou = nomeNovo != null && !nomeNovo.equals(pessoa.getPesNome());
        boolean emailMudou = emailNovo != null && !emailNovo.equals(pessoa.getPesEmail());
        if (nomeMudou || emailMudou) {
            conferirEdicaoDeDadosPessoais(usuario, pessoa);
            pessoaService.atualizarPessoa(pessoaId, new PessoaUpdateRequest(nomeMudou ? nomeNovo : null, null, null,
                    emailMudou ? emailNovo : null, null, null, null, null));
        }

        UserRole papelFinal = papelAtual;
        if (novoPapel != null && novoPapel != papelAtual) {
            if (Objects.equals(pessoaId, usuario.getPesCod())) {
                throw new IllegalArgumentException("Não é possível alterar o próprio papel.");
            }
            atualizarPapelUsuario(pessoaId, condominioId, papelAtual, novoPapel);
            papelFinal = novoPapel;
        }
        return new UsuarioCondominioDTO(vinculoExistente(pessoaId, condominioId, papelFinal));
    }

    /**
     * Manda o link de redefinição de senha para o e-mail da pessoa, que precisa ter vínculo num condomínio que o
     * usuário administra. Devolve o e-mail para onde foi.
     */
    @Transactional
    public String enviarLinkDeSenha(Pessoa usuario, Integer pessoaId, String enderecoWeb) {
        Pessoa pessoa = pessoaAoAlcance(usuario, pessoaId);
        if (isAdministradorGeral(pessoa) && !isAdministradorGeral(usuario)) {
            throw new AccessDeniedException("Só a administração geral redefine a senha de um administrador geral.");
        }
        passwordResetService.createPasswordResetToken(pessoa.getPesEmail(), VALIDADE_LINK_SENHA_HORAS, enderecoWeb);
        return pessoa.getPesEmail();
    }

    /**
     * Define a senha da pessoa na hora, sem link. Vale a mesma regra de alterar o e-mail, que é o login: quem não é
     * administrador geral só define a senha de quem tem acesso apenas a condomínios que ele administra. A própria
     * senha se troca no perfil, conferindo a atual. As sessões abertas da pessoa deixam de valer.
     */
    @Transactional
    public void definirSenha(Pessoa usuario, Integer pessoaId, String novaSenha) {
        if (Objects.equals(pessoaId, usuario.getPesCod())) {
            throw new IllegalArgumentException("Para trocar a sua própria senha, use \"Meu perfil\".");
        }
        Pessoa pessoa = pessoaAoAlcance(usuario, pessoaId);
        if (!isAdministradorGeral(usuario)) {
            if (isAdministradorGeral(pessoa)) {
                throw new AccessDeniedException("Só a administração geral define a senha de um administrador geral.");
            }
            if (temAcessoForaDoAlcance(usuario, pessoa)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta pessoa também tem acesso a condomínio "
                        + "que você não administra; envie o link para ela mesma definir a senha.");
            }
        }
        pessoaService.definirSenha(pessoaId, novaSenha);
    }

    /**
     * Desativa ou reativa o acesso, mantendo o registro e a data em que foi dado. Desativado, o acesso deixa de
     * contar para entrar no sistema e para o que a pessoa vê no condomínio. Ninguém desativa o próprio acesso.
     */
    @Transactional
    public UsuarioCondominioDTO mudarSituacaoDoVinculo(Pessoa usuario, Integer pessoaId, Integer condominioId,
                                                       UserRole papel, boolean ativo) {
        conferirGestaoDeUsuarios(usuario, condominioId);
        if (!ativo && Objects.equals(pessoaId, usuario.getPesCod())) {
            throw new IllegalArgumentException("Não é possível desativar o próprio acesso.");
        }
        UsuarioCondominio vinculo = vinculoExistente(pessoaId, condominioId, papel);
        if (Boolean.valueOf(ativo).equals(vinculo.getUscAtivoAssociacao())) {
            throw new IllegalArgumentException(ativo ? "Este acesso já está ativo." : "Este acesso já está desativado.");
        }
        vinculo.setUscAtivoAssociacao(ativo);
        vinculo.setUscDtAtualizacao(LocalDateTime.now());
        return new UsuarioCondominioDTO(usuarioCondominioRepository.save(vinculo));
    }

    /** Remove o acesso da pessoa ao condomínio com aquele papel. Ninguém remove o próprio acesso. */
    @Transactional
    public void excluirVinculo(Pessoa usuario, Integer pessoaId, Integer condominioId, UserRole papel) {
        conferirGestaoDeUsuarios(usuario, condominioId);
        if (Objects.equals(pessoaId, usuario.getPesCod())) {
            throw new IllegalArgumentException("Não é possível excluir o próprio vínculo de acesso.");
        }
        usuarioCondominioRepository.delete(vinculoExistente(pessoaId, condominioId, papel));
    }

    private UsuarioCondominio vinculoExistente(Integer pessoaId, Integer condominioId, UserRole papel) {
        return usuarioCondominioRepository.findById(new UsuarioCondominioId(pessoaId, condominioId, papel))
                .orElseThrow(() -> new EntityNotFoundException("Vínculo de usuário não encontrado."));
    }

    private Integer unicoCondominioAdministrado(Pessoa usuario) {
        if (!isAdministradorGeral(usuario)) {
            Set<Integer> administrados = condominiosComPapel(usuario, PAPEIS_QUE_GERENCIAM_USUARIOS);
            if (administrados.size() == 1) {
                return administrados.iterator().next();
            }
        }
        throw new IllegalArgumentException("Informe o condomínio.");
    }

    /** A pessoa escolhida na lista de ocupantes precisa ocupar uma unidade do condomínio do novo acesso. */
    private Pessoa ocupanteDoCondominio(Integer pessoaId, Integer condominioId) {
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new IllegalArgumentException("Ocupante não encontrado neste condomínio."));
        boolean ocupaUnidadeDoCondominio = ocupanteRepository.findByPessoa(pessoa).stream()
                .anyMatch(ocupante -> ocupante.getUnidade() != null && ocupante.getUnidade().getCondominio() != null
                        && condominioId.equals(ocupante.getUnidade().getCondominio().getConCod()));
        if (!ocupaUnidadeDoCondominio) {
            throw new IllegalArgumentException("Ocupante não encontrado neste condomínio.");
        }
        return pessoa;
    }

    private Pessoa cadastrarPessoaNova(UsuarioCondominioRequestDTO dados, boolean criarSenha) {
        if (!StringUtils.hasText(dados.getPesCpfCnpj())) {
            throw new IllegalArgumentException("Informe o CPF.");
        }
        if (!StringUtils.hasText(dados.getPesNome())) {
            throw new IllegalArgumentException("Informe o nome.");
        }
        if (!StringUtils.hasText(dados.getPesEmail())) {
            throw new IllegalArgumentException("Informe o e-mail.");
        }
        if (criarSenha && !StringUtils.hasText(dados.getPesSenhaLogin())) {
            throw new IllegalArgumentException("Informe a senha.");
        }
        Pessoa nova = new Pessoa();
        nova.setPesNome(dados.getPesNome().trim());
        nova.setPesCpfCnpj(somenteDigitos(dados.getPesCpfCnpj()));
        nova.setPesEmail(dados.getPesEmail().trim());
        nova.setPesTelefone(StringUtils.hasText(dados.getPesTelefone()) ? dados.getPesTelefone().trim() : null);
        nova.setPesTipo('F');
        nova.setPesAtivo(true);
        nova.setPesIsGlobalAdmin(false);
        if (criarSenha) {
            nova.setPesSenhaLogin(dados.getPesSenhaLogin());
        }
        return pessoaService.cadastrarPessoa(nova);
    }

    /**
     * Quem não é administrador geral não altera nome nem e-mail (que é o login) de administrador geral nem de quem
     * tem acesso a outro condomínio: seria tomar a conta de alguém fora do seu alcance.
     */
    private void conferirEdicaoDeDadosPessoais(Pessoa usuario, Pessoa pessoa) {
        if (isAdministradorGeral(usuario)) {
            return;
        }
        if (isAdministradorGeral(pessoa)) {
            throw new AccessDeniedException("Só a administração geral altera os dados de um administrador geral.");
        }
        if (temAcessoForaDoAlcance(usuario, pessoa)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta pessoa também tem acesso a condomínio que "
                    + "você não administra; o nome e o e-mail dela só podem ser alterados pela administração geral.");
        }
    }

    /** Se a pessoa tem acesso a algum condomínio que o usuário não administra. */
    private boolean temAcessoForaDoAlcance(Pessoa usuario, Pessoa pessoa) {
        Set<Integer> administrados = condominiosComPapel(usuario, PAPEIS_QUE_GERENCIAM_USUARIOS);
        return usuarioCondominioRepository.findByPesCod(pessoa.getPesCod()).stream()
                .anyMatch(vinculo -> !administrados.contains(vinculo.getConCod()));
    }

    /** A pessoa, desde que tenha acesso a algum condomínio que o usuário administra. */
    private Pessoa pessoaAoAlcance(Pessoa usuario, Integer pessoaId) {
        if (!isAdministradorGeral(usuario)) {
            Set<Integer> administrados = condominiosComPapel(usuario, PAPEIS_QUE_GERENCIAM_USUARIOS);
            boolean alcanca = usuarioCondominioRepository.findByPesCod(pessoaId).stream()
                    .anyMatch(vinculo -> administrados.contains(vinculo.getConCod()));
            if (!alcanca) {
                throw new AccessDeniedException("Esta pessoa não tem acesso a um condomínio que você administra.");
            }
        }
        return pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada."));
    }

    private Optional<Pessoa> pessoaPorDocumento(String cpfCnpj) {
        if (!StringUtils.hasText(cpfCnpj)) {
            return Optional.empty();
        }
        String digitos = somenteDigitos(cpfCnpj);
        Optional<Pessoa> pessoa = digitos.isEmpty() ? Optional.empty() : pessoaRepository.findByPesCpfCnpj(digitos);
        return pessoa.isPresent() ? pessoa : pessoaRepository.findByPesCpfCnpj(cpfCnpj.trim());
    }

    private static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("[^0-9]", "");
    }
}