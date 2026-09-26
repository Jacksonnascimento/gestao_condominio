package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.OcupanteRequestDTO;
import br.com.gestaocondominio.api.controller.dto.OcupanteResponseDTO;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.OcupanteSpecification;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service("ocupanteService")
public class OcupanteService {

    @Autowired
    private OcupanteRepository ocupanteRepository;
    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private UnidadeRepository unidadeRepository;
    @Autowired
    private PessoaService pessoaService;
    @Autowired
    private UsuarioCondominioService usuarioCondominioService;

    @Transactional(readOnly = true)
    public List<OcupanteResponseDTO> consultarOcupantesPorUsuario(Pessoa usuario, Integer condominioId, String busca,
            OcupanteVinculo vinculo, Integer unidadeId) {
        List<Ocupante> ocupantes = findOcupantesByUsuario(usuario, condominioId, busca, vinculo, unidadeId);

        ocupantes.sort(Comparator.comparing(o -> (o.getPessoa() != null ? o.getPessoa().getPesNome() : ""),
                String.CASE_INSENSITIVE_ORDER));

        return ocupantes.stream()
                .map(OcupanteResponseDTO::new)
                .collect(Collectors.toList());
    }

    private List<Ocupante> findOcupantesByUsuario(Pessoa usuario, Integer condominioId, String busca,
            OcupanteVinculo vinculo, Integer unidadeId) {
        if (usuario.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuario, UserRole.SINDICO,
                UserRole.ADMIN, UserRole.FUNCIONARIO_ADM)) {
            Specification<Ocupante> spec = OcupanteSpecification.comFiltros(condominioId, busca, vinculo, unidadeId);
            return ocupanteRepository.findAll(spec); 
        } else {
            List<Unidade> unidadesDoMorador = findUnidadesByMorador(usuario);
            if (!unidadesDoMorador.isEmpty()) {
                if (unidadeId != null) {
                    boolean temAcesso = unidadesDoMorador.stream().anyMatch(u -> u.getUniCod().equals(unidadeId));
                    if (temAcesso) {
                        Specification<Ocupante> spec = OcupanteSpecification.comFiltros(null, busca, vinculo,
                                unidadeId);
                        return ocupanteRepository.findAll(spec); 
                    }
                } else {
                    Specification<Ocupante> spec = OcupanteSpecification.comFiltros(null, busca, vinculo, null)
                            .and((root, query, cb) -> root.get("unidade").in(unidadesDoMorador));
                    return ocupanteRepository.findAll(spec);
                }
            }
        }
        return Collections.emptyList();
    }

    public List<OcupanteResponseDTO> consultarOcupantesPorUsuario(Pessoa usuario, Integer condominioId, String busca,
            OcupanteVinculo vinculo) {
        return this.consultarOcupantesPorUsuario(usuario, condominioId, busca, vinculo, null);
    }

    @Transactional(readOnly = true)
    public Map<OcupanteVinculo, Long> contarOcupantesPorUsuario(Pessoa usuario, Integer condominioId) {
        List<Ocupante> ocupantes = findOcupantesByUsuario(usuario, condominioId, null, null, null);
        return ocupantes.stream()
                .collect(Collectors.groupingBy(Ocupante::getOcuVinculo, Collectors.counting()));
    }

    @Transactional
    public OcupanteResponseDTO cadastrarOcupante(OcupanteRequestDTO dto) {
        if (dto.getUnidadeId() == null || dto.getVinculo() == null || dto.getInicioOcupacao() == null
                || dto.getPesCpfCnpj() == null || dto.getPesCpfCnpj().isBlank()) {
            throw new IllegalArgumentException("CPF/CNPJ, Unidade, Vínculo e Início da Ocupação são obrigatórios.");
        }

        Pessoa pessoa = pessoaRepository.findByPesCpfCnpj(dto.getPesCpfCnpj())
                .orElseGet(() -> {
                    Pessoa novaPessoa = new Pessoa();
                    novaPessoa.setPesNome(dto.getPesNome());
                    novaPessoa.setPesCpfCnpj(dto.getPesCpfCnpj());
                    novaPessoa.setPesTipo(dto.getPesTipo());
                    novaPessoa.setPesEmail(dto.getPesEmail());
                    novaPessoa.setPesTelefone(dto.getPesTelefone());
                    return pessoaService.cadastrarPessoa(novaPessoa);
                });

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Unidade não encontrada com o ID: " + dto.getUnidadeId()));

        ocupanteRepository.findByPessoaAndUnidade(pessoa, unidade).ifPresent(m -> {
            throw new IllegalArgumentException("Esta pessoa já está cadastrada como ocupante desta unidade.");
        });

        Ocupante novoOcupante = new Ocupante();
        novoOcupante.setPessoa(pessoa);
        novoOcupante.setUnidade(unidade);
        novoOcupante.setOcuVinculo(dto.getVinculo());
        novoOcupante.setOcuDtInicioOcupacao(dto.getInicioOcupacao());
        novoOcupante.setOcuDtFimOcupacao(dto.getFimOcupacao());

        if (dto.getVinculo() == OcupanteVinculo.MULTIPROPRIETARIO) {
            novoOcupante.setOcuPeriodoUso(dto.getPeriodoUso());
            novoOcupante.setOcuTipoPeriodo(dto.getTipoPeriodo());
        }

        novoOcupante.setOcuDtCadastro(LocalDateTime.now());
        novoOcupante.setOcuDtAtualizacao(LocalDateTime.now());

        Ocupante ocupanteSalvo = ocupanteRepository.save(novoOcupante);
        return new OcupanteResponseDTO(ocupanteSalvo);
    }

    @Transactional
    public OcupanteResponseDTO editarOcupante(Integer id, OcupanteRequestDTO dto, Pessoa usuarioLogado) {
        Ocupante ocupanteExistente = buscarPorIdEValidarAcesso(id, usuarioLogado);
        return new OcupanteResponseDTO(aplicarEdicao(ocupanteExistente, dto));
    }

    private Ocupante aplicarEdicao(Ocupante ocupanteExistente, OcupanteRequestDTO dto) {
        Pessoa pessoaParaAtualizar = ocupanteExistente.getPessoa();
        pessoaParaAtualizar.setPesNome(dto.getPesNome());
        pessoaParaAtualizar.setPesEmail(dto.getPesEmail());
        pessoaParaAtualizar.setPesTelefone(dto.getPesTelefone());
        pessoaRepository.save(pessoaParaAtualizar);

        ocupanteExistente.setOcuVinculo(dto.getVinculo());
        ocupanteExistente.setOcuDtInicioOcupacao(dto.getInicioOcupacao());
        ocupanteExistente.setOcuDtFimOcupacao(dto.getFimOcupacao());

        if (dto.getVinculo() == OcupanteVinculo.MULTIPROPRIETARIO) {
            ocupanteExistente.setOcuPeriodoUso(dto.getPeriodoUso());
            ocupanteExistente.setOcuTipoPeriodo(dto.getTipoPeriodo());
        } else {
            ocupanteExistente.setOcuPeriodoUso(null);
            ocupanteExistente.setOcuTipoPeriodo(null);
        }

        ocupanteExistente.setOcuDtAtualizacao(LocalDateTime.now());
        return ocupanteRepository.save(ocupanteExistente);
    }

    @Transactional
    public void excluirOcupante(Integer id, Pessoa usuarioLogado) {
        Ocupante ocupante = buscarPorIdEValidarAcesso(id, usuarioLogado);
        ocupanteRepository.delete(ocupante);
    }

    @Transactional(readOnly = true)
    public Ocupante buscarPorIdEValidarAcesso(Integer id, Pessoa usuario) {
        Ocupante ocupante = ocupanteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocupante não encontrado"));

        if (usuario.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuario, UserRole.SINDICO,
                UserRole.ADMIN, UserRole.FUNCIONARIO_ADM)) {
            return ocupante;
        }

        if (usuarioCondominioService.possuiRole(usuario, UserRole.MORADOR)) {
            boolean pertence = findUnidadesByMorador(usuario).stream()
                    .anyMatch(unidade -> unidade.getUniCod().equals(ocupante.getUnidade().getUniCod())); // Comparar IDs
            if (pertence) {
                return ocupante;
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso Negado");
    }

    @Transactional(readOnly = true)
    public List<Unidade> findUnidadesByMorador(Pessoa morador) {
        return ocupanteRepository.findByPessoa(morador)
                .stream()
                .map(Ocupante::getUnidade)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Ocupante> findOcupantesSemLoginMoradorByCondominio(Integer condominioId) {
        return ocupanteRepository.findOcupantesSemLoginMoradorByCondominio(condominioId);
    }

    @Transactional(readOnly = true)
    public List<OcupanteResponseDTO> findOcupantesDtoSemLoginMoradorByCondominio(Integer condominioId) {
        List<Ocupante> ocupantes = ocupanteRepository.findOcupantesSemLoginMoradorByCondominio(condominioId);
        return ocupantes.stream()
                .map(OcupanteResponseDTO::new)
                .collect(Collectors.toList());
    }

    // --- Usados pela API v1. Diferente dos métodos acima, a gestão vale só nos condomínios em que a pessoa é
    // síndico, administradora ou funcionário administrativo, e não em qualquer condomínio.

    /**
     * Ocupantes que a pessoa pode ver, em ordem alfabética: todos para o administrador geral; os dos condomínios em
     * que ela é da gestão; e os das unidades que ela ocupa.
     */
    @Transactional(readOnly = true)
    public Page<Ocupante> consultarOcupantes(Pessoa usuario, Integer condominioId, String busca,
                                             OcupanteVinculo vinculo, Integer unidadeId, Pageable pageable) {
        return ocupanteRepository.findAll(
                especificacaoVisivel(usuario, condominioId, busca, vinculo, unidadeId), pageable);
    }

    /** Quantidade por vínculo (e o {@code TOTAL}), com os mesmos filtros de {@link #consultarOcupantes}. */
    @Transactional(readOnly = true)
    public Map<String, Long> contarOcupantesPorVinculo(Pessoa usuario, Integer condominioId, String busca,
                                                       OcupanteVinculo vinculo, Integer unidadeId) {
        List<Ocupante> ocupantes = ocupanteRepository.findAll(
                especificacaoVisivel(usuario, condominioId, busca, vinculo, unidadeId));
        Map<String, Long> totais = new LinkedHashMap<>();
        totais.put("TOTAL", (long) ocupantes.size());
        for (OcupanteVinculo tipo : OcupanteVinculo.values()) {
            totais.put(tipo.name(), ocupantes.stream().filter(o -> o.getOcuVinculo() == tipo).count());
        }
        return totais;
    }

    /** Um ocupante, conferindo se a pessoa pode vê-lo (mesma regra de {@link #consultarOcupantes}). */
    @Transactional(readOnly = true)
    public Ocupante buscarOcupanteVisivel(Integer id, Pessoa usuario) {
        Ocupante ocupante = buscarComDetalhes(id);
        if (podeGerenciarOcupantesDoCondominio(ocupante.getUnidade().getCondominio().getConCod())
                || ocupanteRepository.findByPessoaAndUnidade(usuario, ocupante.getUnidade()).isPresent()) {
            return ocupante;
        }
        throw new AccessDeniedException("Acesso negado. Você não tem permissão para visualizar este ocupante.");
    }

    /** Se quem está logado cadastra ocupantes em pelo menos um condomínio. */
    public boolean podeGerenciarOcupantes() {
        return AcessoPorCondominio.temPapelEmAlgumCondominio(AcessoPorCondominio.GESTAO);
    }

    public boolean podeGerenciarOcupantesDoCondominio(Integer conCod) {
        return AcessoPorCondominio.temPapelNoCondominio(conCod, AcessoPorCondominio.GESTAO);
    }

    /**
     * Vincula uma pessoa a uma unidade de um condomínio que quem está logado gerencia. Se o CPF/CNPJ ainda não tem
     * cadastro, a pessoa é cadastrada com o nome, o e-mail e o telefone informados; se já tem, vale o cadastro dela.
     */
    @Transactional
    public Ocupante cadastrarOcupanteComoGestor(OcupanteRequestDTO dto) {
        if (dto.getUnidadeId() == null) {
            throw new IllegalArgumentException("Informe a unidade.");
        }
        Unidade unidade = unidadeRepository.findByIdWithCondominio(dto.getUnidadeId())
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));
        if (!podeGerenciarOcupantesDoCondominio(unidade.getCondominio().getConCod())) {
            throw new AccessDeniedException("Acesso negado. Você não gerencia os ocupantes deste condomínio.");
        }
        if (!Boolean.TRUE.equals(unidade.getUniAtiva())) {
            throw new IllegalArgumentException("A unidade está inativa.");
        }
        validarPeriodo(dto);

        dto.setPesCpfCnpj(somenteDigitos(dto.getPesCpfCnpj()));
        boolean pessoaNova = pessoaRepository.findByPesCpfCnpj(dto.getPesCpfCnpj()).isEmpty();
        if (pessoaNova && (!StringUtils.hasText(dto.getPesNome()) || !StringUtils.hasText(dto.getPesEmail()))) {
            throw new IllegalArgumentException("Para cadastrar uma pessoa nova, informe o nome e o e-mail.");
        }
        return buscarComDetalhes(cadastrarOcupante(dto).id());
    }

    /**
     * Altera o vínculo e os dados de contato do ocupante. O e-mail é o login de quem acessa o sistema: o de uma
     * pessoa com acesso só pode ser trocado pelo administrador geral, para que a gestão de um condomínio não consiga
     * redirecionar a redefinição de senha de outra pessoa para si.
     */
    @Transactional
    public Ocupante editarOcupanteComoGestor(Integer id, OcupanteRequestDTO dto) {
        Ocupante ocupante = buscarOcupanteGerenciavel(id);
        validarPeriodo(dto);

        Pessoa pessoa = ocupante.getPessoa();
        String novoEmail = dto.getPesEmail() == null ? null : dto.getPesEmail().trim();
        if (novoEmail != null && !novoEmail.equals(pessoa.getPesEmail())) {
            if (!AcessoPorCondominio.administradorGeral() && temAcessoAoSistema(pessoa)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Esta pessoa acessa o sistema com este e-mail; só o administrador geral pode alterá-lo.");
            }
            pessoaRepository.findByPesEmail(novoEmail)
                    .filter(outra -> !outra.getPesCod().equals(pessoa.getPesCod()))
                    .ifPresent(outra -> {
                        throw new IllegalArgumentException("E-mail já cadastrado para outra pessoa.");
                    });
            dto.setPesEmail(novoEmail);
        }
        aplicarEdicao(ocupante, dto);
        return buscarComDetalhes(id);
    }

    @Transactional
    public void excluirOcupanteComoGestor(Integer id) {
        ocupanteRepository.delete(buscarOcupanteGerenciavel(id));
    }

    private Ocupante buscarOcupanteGerenciavel(Integer id) {
        Ocupante ocupante = buscarComDetalhes(id);
        if (!podeGerenciarOcupantesDoCondominio(ocupante.getUnidade().getCondominio().getConCod())) {
            throw new AccessDeniedException("Acesso negado. Você não gerencia os ocupantes deste condomínio.");
        }
        return ocupante;
    }

    private Ocupante buscarComDetalhes(Integer id) {
        return ocupanteRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Ocupante não encontrado."));
    }

    private Specification<Ocupante> especificacaoVisivel(Pessoa usuario, Integer condominioId, String busca,
                                                         OcupanteVinculo vinculo, Integer unidadeId) {
        Specification<Ocupante> filtros = OcupanteSpecification.paraListagem(condominioId, busca, vinculo, unidadeId);
        if (AcessoPorCondominio.administradorGeral()) {
            return filtros;
        }
        Set<Integer> unidadesOcupadas = findUnidadesByMorador(usuario).stream()
                .map(Unidade::getUniCod)
                .collect(Collectors.toSet());
        return filtros.and(OcupanteSpecification.visiveis(
                AcessoPorCondominio.condominiosComPapel(AcessoPorCondominio.GESTAO), unidadesOcupadas));
    }

    private boolean temAcessoAoSistema(Pessoa pessoa) {
        return Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin()) || pessoa.getPesSenhaLogin() != null
                || !usuarioCondominioService.findByPessoa(pessoa).isEmpty();
    }

    private static void validarPeriodo(OcupanteRequestDTO dto) {
        if (dto.getInicioOcupacao() != null && dto.getFimOcupacao() != null
                && dto.getInicioOcupacao().isAfter(dto.getFimOcupacao())) {
            throw new IllegalArgumentException("A data de início da ocupação não pode ser posterior à data de fim.");
        }
    }

    private static String somenteDigitos(String documento) {
        return documento == null ? null : documento.replaceAll("[^0-9]", "");
    }
}