package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.VisitanteRequestDTO;
import br.com.gestaocondominio.api.controller.v1.dto.VisitanteDTOs.MoradorOpcao;
import br.com.gestaocondominio.api.controller.v1.dto.VisitanteDTOs.UnidadeOpcao;
import br.com.gestaocondominio.api.controller.v1.dto.VisitanteDTOs.VisitanteDetalhe;
import br.com.gestaocondominio.api.controller.v1.dto.VisitanteDTOs.VisitanteResumo;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.entity.Visitante;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.enums.VisitanteStatus;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.VisitanteRepository;
import br.com.gestaocondominio.api.domain.repository.VisitanteSpecification;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import br.com.gestaocondominio.api.util.ValidadorDocumento;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VisitanteService {

    /** Papéis que registram e alteram visitantes no condomínio: administração e portaria. */
    private static final Set<UserRole> PAPEIS_QUE_GERENCIAM =
            EnumSet.of(UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO);

    private final VisitanteRepository visitanteRepository;
    private final UnidadeRepository unidadeRepository;
    private final CondominioRepository condominioRepository;
    private final UsuarioCondominioService usuarioCondominioService;
    private final OcupanteRepository ocupanteRepository;

    public VisitanteService(VisitanteRepository visitanteRepository,
                            UnidadeRepository unidadeRepository,
                            CondominioRepository condominioRepository,
                            UsuarioCondominioService usuarioCondominioService,
                            OcupanteRepository ocupanteRepository) {
        this.visitanteRepository = visitanteRepository;
        this.unidadeRepository = unidadeRepository;
        this.condominioRepository = condominioRepository;
        this.usuarioCondominioService = usuarioCondominioService;
        this.ocupanteRepository = ocupanteRepository;
    }

    private boolean isAdminGeral(Pessoa pessoa) {
        return Boolean.TRUE.equals(pessoa.getPesIsGlobalAdmin());
    }

    /**
     * Condomínios em que a pessoa tem vínculo ativo de administração ou portaria. Só nesses ela registra e altera
     * visitantes (e vê todos os visitantes do condomínio).
     */
    public Set<Integer> condominiosQueGerencia(Pessoa pessoa) {
        return usuarioCondominioService.findByPessoa(pessoa).stream()
                .filter(v -> Boolean.TRUE.equals(v.getUscAtivoAssociacao()) && PAPEIS_QUE_GERENCIAM.contains(v.getUscPapel()))
                .map(UsuarioCondominio::getConCod)
                .collect(Collectors.toSet());
    }

    /** Se a pessoa pode registrar visitantes em algum condomínio (mostra o botão "Novo Visitante"). */
    public boolean podeGerenciarVisitantes(Pessoa pessoa) {
        return isAdminGeral(pessoa) || !condominiosQueGerencia(pessoa).isEmpty();
    }

    /** Se a pessoa pode editar o visitante e registrar a saída dele. */
    private boolean podeAlterar(Visitante visitante, Pessoa pessoa) {
        return isAdminGeral(pessoa) || condominiosQueGerencia(pessoa).contains(visitante.getCondominio().getConCod());
    }

    /** Unidades de que a pessoa é ocupante; o morador vê os visitantes delas. */
    private Set<Integer> unidadesOcupadas(Pessoa pessoa) {
        return ocupanteRepository.findByPessoa(pessoa).stream()
                .map(o -> o.getUnidade().getUniCod())
                .collect(Collectors.toSet());
    }

    /**
     * Administrador geral vê tudo (e pode filtrar por condomínio). Os demais veem os visitantes dos condomínios em
     * que trabalham na administração ou na portaria e os das unidades que ocupam; o filtro de condomínio só estreita
     * esse conjunto, nunca o amplia.
     */
    private Specification<Visitante> getSpec(Pessoa usuarioLogado, Integer condominioId, String nome, Integer unidadeId) {
        Specification<Visitante> filtros = VisitanteSpecification.filtrar(condominioId, nome, unidadeId);
        if (isAdminGeral(usuarioLogado)) {
            return filtros;
        }
        return filtros.and(VisitanteSpecification.visiveisPara(condominiosQueGerencia(usuarioLogado),
                unidadesOcupadas(usuarioLogado)));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> contarVisitantes(Pessoa usuarioLogado, Integer condominioId, String nome, Integer unidadeId) {
        Specification<Visitante> spec = getSpec(usuarioLogado, condominioId, nome, unidadeId);
        List<Visitante> visitantes = visitanteRepository.findAll(spec);

        LocalDateTime inicioDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime fimDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        long total = visitantes.size();
        long noLocal = visitantes.stream().filter(v -> v.getStatus() == VisitanteStatus.NO_LOCAL).count();

        long visitantesDoDia = visitantes.stream()
                .filter(v -> v.getDataEntrada().isAfter(inicioDoDia) && v.getDataEntrada().isBefore(fimDoDia))
                .count();

        long saidasDoDia = visitantes.stream()
                .filter(v -> v.getDataSaida() != null && v.getDataSaida().isAfter(inicioDoDia) && v.getDataSaida().isBefore(fimDoDia))
                .count();

        return Map.of(
                "TOTAL", total,
                "NO_LOCAL", noLocal,
                "DO_DIA", visitantesDoDia,
                "SAIDAS_DIA", saidasDoDia
        );
    }

    @Transactional
    public Visitante cadastrarVisitante(VisitanteRequestDTO dto, Pessoa usuarioLogado) {
        if (!podeGerenciarVisitantes(usuarioLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }

        if (StringUtils.hasText(dto.getCpf())) {
            if (!ValidadorDocumento.isValid(dto.getCpf())) {
                throw new IllegalArgumentException("O CPF informado é inválido.");
            }
        }

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));

        // Sem condomínio informado (a API não pede), vale o da unidade.
        Integer condoId = dto.getCondominioId() != null ? dto.getCondominioId() : unidade.getCondominio().getConCod();

        Condominio condominio = condominioRepository.findById(condoId)
                .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado."));

        if (!unidade.getCondominio().getConCod().equals(condominio.getConCod())) {
            throw new IllegalArgumentException("A unidade não pertence ao condomínio selecionado.");
        }

        // Quem é da administração ou da portaria de um condomínio não registra visitantes em outro.
        if (!isAdminGeral(usuarioLogado) && !condominiosQueGerencia(usuarioLogado).contains(condominio.getConCod())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode registrar visitantes neste condomínio.");
        }

        Pessoa morador = moradorDaUnidade(dto.getMoradorId(), unidade, null);

        Visitante visitante = new Visitante();
        visitante.setCondominio(condominio);
        visitante.setUnidade(unidade);
        visitante.setPessoaRegistro(usuarioLogado);
        visitante.setMoradorAutorizou(morador);
        visitante.setNome(dto.getNome());
        visitante.setCpf(dto.getCpf());
        visitante.setRg(dto.getRg());
        visitante.setTelefone(dto.getTelefone());
        visitante.setObservacoes(dto.getObservacoes());
        visitante.setStatus(VisitanteStatus.NO_LOCAL);
        visitante.setDataEntrada(LocalDateTime.now());
        visitante.setDataCadastro(LocalDateTime.now());
        visitante.setDataAtualizacao(LocalDateTime.now());

        return visitanteRepository.save(visitante);
    }

    @Transactional
    public Visitante atualizarVisitante(Integer id, VisitanteRequestDTO dto, Pessoa usuarioLogado) {
        Visitante visitante = buscarPorIdEValidarAcesso(id, usuarioLogado, true);

        if (StringUtils.hasText(dto.getCpf())) {
            if (!ValidadorDocumento.isValid(dto.getCpf())) {
                throw new IllegalArgumentException("O CPF informado é inválido.");
            }
        }

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));

        if (!unidade.getCondominio().getConCod().equals(visitante.getCondominio().getConCod())) {
             throw new IllegalArgumentException("A unidade deve pertencer ao mesmo condomínio.");
        }

        Pessoa morador = moradorDaUnidade(dto.getMoradorId(), unidade, visitante.getMoradorAutorizou());

        visitante.setNome(dto.getNome());
        visitante.setCpf(dto.getCpf());
        visitante.setRg(dto.getRg());
        visitante.setTelefone(dto.getTelefone());
        visitante.setUnidade(unidade);
        visitante.setMoradorAutorizou(morador);
        visitante.setObservacoes(dto.getObservacoes());
        visitante.setDataAtualizacao(LocalDateTime.now());

        return visitanteRepository.save(visitante);
    }

    @Transactional
    public Visitante registrarSaida(Integer id, Pessoa usuarioLogado) {
        Visitante visitante = buscarPorIdEValidarAcesso(id, usuarioLogado, true);

        if (visitante.getStatus() == VisitanteStatus.SAIU) {
            throw new IllegalArgumentException("Saída já registrada para este visitante.");
        }

        visitante.setStatus(VisitanteStatus.SAIU);
        visitante.setDataSaida(LocalDateTime.now());
        visitante.setDataAtualizacao(LocalDateTime.now());

        return visitanteRepository.save(visitante);
    }

    /**
     * O morador que autorizou a entrada precisa ser ocupante da unidade visitada. Na edição, quem já estava no
     * registro continua aceito mesmo que tenha deixado a unidade depois.
     */
    private Pessoa moradorDaUnidade(Integer moradorId, Unidade unidade, Pessoa moradorAtual) {
        if (moradorId == null) {
            return null;
        }
        if (moradorAtual != null && moradorId.equals(moradorAtual.getPesCod())) {
            return moradorAtual;
        }
        return ocupanteRepository.findByUnidade(unidade).stream()
                .map(Ocupante::getPessoa)
                .filter(p -> moradorId.equals(p.getPesCod()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("O morador informado não é ocupante da unidade."));
    }

    @Transactional(readOnly = true)
    public Visitante buscarPorIdEValidarAcesso(Integer id, Pessoa usuarioLogado, boolean paraEscrita) {
        Visitante visitante = visitanteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Visitante não encontrado."));

        // Administração e portaria do condomínio do visitante leem e alteram.
        if (podeAlterar(visitante, usuarioLogado)) {
            return visitante;
        }

        // O ocupante da unidade visitada só lê.
        if (!paraEscrita && unidadesOcupadas(usuarioLogado).contains(visitante.getUnidade().getUniCod())) {
            return visitante;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
    }

    // --- Usados pela API /api/v1/visitantes ---

    /**
     * Listagem da API, com as mesmas regras de visibilidade da tela; sem CPF, RG e observações. Com {@code status},
     * só os visitantes nessa situação (os que estão no condomínio ou os que já saíram).
     */
    @Transactional(readOnly = true)
    public Page<VisitanteResumo> consultarResumos(Pessoa usuarioLogado, Integer condominioId, String nome,
                                                  Integer unidadeId, VisitanteStatus status, Pageable pageable) {
        Specification<Visitante> spec = getSpec(usuarioLogado, condominioId, nome, unidadeId);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        Page<Visitante> page = visitanteRepository.findAll(spec, pageable);
        boolean adminGeral = isAdminGeral(usuarioLogado);
        Set<Integer> gerenciados = condominiosQueGerencia(usuarioLogado);
        return page.map(v -> VisitanteResumo.de(v, adminGeral || gerenciados.contains(v.getCondominio().getConCod())));
    }

    /** Um visitante para a API; CPF, RG e observações só para quem pode alterá-lo. */
    @Transactional(readOnly = true)
    public VisitanteDetalhe buscarDetalhe(Integer id, Pessoa usuarioLogado) {
        Visitante visitante = buscarPorIdEValidarAcesso(id, usuarioLogado, false);
        return VisitanteDetalhe.de(visitante, podeAlterar(visitante, usuarioLogado));
    }

    /**
     * Unidades ativas para o filtro e o formulário: as dos condomínios que a pessoa gerencia e as que ela ocupa,
     * limitadas ao condomínio informado. O administrador geral precisa informar o condomínio.
     */
    @Transactional(readOnly = true)
    public List<UnidadeOpcao> unidadesDisponiveis(Pessoa usuarioLogado, Integer condominioId) {
        Map<Integer, Unidade> unidades = new LinkedHashMap<>();
        if (isAdminGeral(usuarioLogado)) {
            if (condominioId != null) {
                unidadeRepository.findAtivasByCondominioConCodWithCondominio(condominioId)
                        .forEach(u -> unidades.put(u.getUniCod(), u));
            }
        } else {
            condominiosQueGerencia(usuarioLogado).stream()
                    .filter(c -> condominioId == null || c.equals(condominioId))
                    .flatMap(c -> unidadeRepository.findAtivasByCondominioConCodWithCondominio(c).stream())
                    .forEach(u -> unidades.put(u.getUniCod(), u));
            ocupanteRepository.findByPessoa(usuarioLogado).stream()
                    .map(Ocupante::getUnidade)
                    .filter(u -> Boolean.TRUE.equals(u.getUniAtiva()))
                    .filter(u -> condominioId == null || u.getCondominio().getConCod().equals(condominioId))
                    .forEach(u -> unidades.putIfAbsent(u.getUniCod(), u));
        }
        return unidades.values().stream()
                .sorted(Comparator.comparing((Unidade u) -> Objects.toString(u.getBloco(), ""))
                        .thenComparing(u -> Objects.toString(u.getUniNumero(), "")))
                .map(UnidadeOpcao::de)
                .toList();
    }

    /** Ocupantes da unidade, para indicar quem autorizou a entrada. Só para quem registra visitantes ali. */
    @Transactional(readOnly = true)
    public List<MoradorOpcao> moradoresDaUnidade(Integer unidadeId, Pessoa usuarioLogado) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));
        if (!isAdminGeral(usuarioLogado)
                && !condominiosQueGerencia(usuarioLogado).contains(unidade.getCondominio().getConCod())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        List<MoradorOpcao> moradores = new ArrayList<>(ocupanteRepository.findByUnidade(unidade).stream()
                .map(MoradorOpcao::de)
                .toList());
        moradores.sort(Comparator.comparing(MoradorOpcao::nome,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return moradores;
    }
}
