package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.v1.dto.DashboardDTOs.Painel;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.enums.OcorrenciaStatus;
import br.com.gestaocondominio.api.domain.enums.StatusContrato;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Indicadores do painel inicial (dashboard). O painel é da administração (síndico, administradora, funcionário
 * administrativo) e da portaria; o morador vai direto para as suas unidades.
 * <p>
 * Cada indicador conta só o que a pessoa veria no módulo correspondente, dentro dos condomínios em que trabalha:
 * onde ela é da administração, conta tudo do condomínio; onde é só da portaria, conta como os módulos de unidades,
 * ocupantes e ocorrências tratam o porteiro (as unidades que ele ocupa e as ocorrências que registrou), e contratos,
 * que a portaria não acessa, não aparecem. O administrador geral vê todos os condomínios.
 */
@Service
public class DashboardService {

    private static final Set<UserRole> PAPEIS_DE_GESTAO =
            EnumSet.of(UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM);

    @PersistenceContext
    private EntityManager entityManager;

    private final UsuarioCondominioService usuarioCondominioService;
    private final OcupanteRepository ocupanteRepository;

    public DashboardService(UsuarioCondominioService usuarioCondominioService, OcupanteRepository ocupanteRepository) {
        this.usuarioCondominioService = usuarioCondominioService;
        this.ocupanteRepository = ocupanteRepository;
    }

    /**
     * Monta o painel de quem está logado. Com {@code condominioId}, só daquele condomínio, que precisa ser um em que
     * a pessoa trabalha (o administrador geral escolhe qualquer um).
     *
     * @throws ResponseStatusException 403 para quem não é da administração nem da portaria, ou para condomínio alheio
     */
    @Transactional(readOnly = true)
    public Painel montarPainel(Pessoa usuario, Integer condominioId) {
        Escopo escopo = escopoDe(usuario, condominioId);
        Escopo escopoDaGestao = escopo.somenteGestao();

        Long unidades = contar("select count(u) from Unidade u where u.uniAtiva = true and ",
                escopo, "u.condominio.conCod", "u.uniCod", null, Map.of());

        Long ocupantes = contar("select count(o) from Ocupante o where ",
                escopo, "o.unidade.condominio.conCod", "o.unidade.uniCod", null, Map.of());

        // Mesma regra de ContratoService.calcularStatus: "Ativo" é o não rescindido com mais de 30 dias até o fim.
        Long contratosAtivos = escopoDaGestao.vazio() ? null : contar(
                "select count(c) from Contrato c where c.status <> :rescindido "
                        + "and (c.dataFim is null or c.dataFim >= :limiteAtivo) and ",
                escopoDaGestao, "c.condominio.conCod", null, null,
                Map.of("rescindido", StatusContrato.RESCINDIDO, "limiteAtivo", LocalDate.now().plusDays(30)));

        Long ocorrenciasPendentes = contar("select count(o) from Ocorrencia o where o.status in :pendentes and ",
                escopo, "o.condominio.conCod", "o.unidade.uniCod", "o.pessoaRegistro.pesCod",
                Map.of("pendentes", List.of(OcorrenciaStatus.ABERTA, OcorrenciaStatus.EM_ANALISE)));

        return new Painel(unidades, ocupantes, contratosAtivos, ocorrenciasPendentes);
    }

    private Escopo escopoDe(Pessoa usuario, Integer condominioId) {
        if (Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin())) {
            return condominioId == null
                    ? new Escopo(true, Set.of(), Set.of(), Set.of(), usuario.getPesCod())
                    : new Escopo(false, Set.of(condominioId), Set.of(), Set.of(), usuario.getPesCod());
        }

        List<UsuarioCondominio> vinculos = usuarioCondominioService.findByPessoa(usuario).stream()
                .filter(v -> Boolean.TRUE.equals(v.getUscAtivoAssociacao()))
                .toList();
        Set<Integer> gestao = vinculos.stream()
                .filter(v -> PAPEIS_DE_GESTAO.contains(v.getUscPapel()))
                .map(UsuarioCondominio::getConCod)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Integer> portaria = vinculos.stream()
                .filter(v -> v.getUscPapel() == UserRole.PORTEIRO && !gestao.contains(v.getConCod()))
                .map(UsuarioCondominio::getConCod)
                .collect(Collectors.toCollection(HashSet::new));

        if (gestao.isEmpty() && portaria.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O painel é exclusivo da administração e da portaria do condomínio.");
        }
        if (condominioId != null) {
            if (!gestao.contains(condominioId) && !portaria.contains(condominioId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a este condomínio.");
            }
            gestao.retainAll(Set.of(condominioId));
            portaria.retainAll(Set.of(condominioId));
        }

        Set<Integer> unidadesProprias = portaria.isEmpty() ? Set.of() : ocupanteRepository.findByPessoa(usuario).stream()
                .map(o -> o.getUnidade().getUniCod())
                .collect(Collectors.toSet());
        return new Escopo(false, gestao, portaria, unidadesProprias, usuario.getPesCod());
    }

    /**
     * Executa a contagem acrescentando a restrição do escopo. Os trechos de JPQL são fixos, desta classe; os valores
     * vão sempre por parâmetro.
     */
    private Long contar(String consultaSemEscopo, Escopo escopo, String caminhoCondominio, String caminhoUnidade,
                        String caminhoAutor, Map<String, Object> parametros) {
        Map<String, Object> todos = new HashMap<>(parametros);
        String restricao = restricao(escopo, caminhoCondominio, caminhoUnidade, caminhoAutor, todos);
        TypedQuery<Long> consulta = entityManager.createQuery(consultaSemEscopo + restricao, Long.class);
        todos.forEach(consulta::setParameter);
        return consulta.getSingleResult();
    }

    /**
     * "(condomínio da administração) ou (condomínio da portaria e (unidade própria ou registrado pela pessoa))". O
     * caminho do autor só existe nas ocorrências, que o morador e o porteiro também veem quando as registraram.
     */
    private String restricao(Escopo escopo, String caminhoCondominio, String caminhoUnidade, String caminhoAutor,
                             Map<String, Object> parametros) {
        if (escopo.todos()) {
            return "1 = 1";
        }
        List<String> alternativas = new ArrayList<>();
        if (!escopo.gestao().isEmpty()) {
            alternativas.add(caminhoCondominio + " in :condominiosDaGestao");
            parametros.put("condominiosDaGestao", escopo.gestao());
        }
        if (!escopo.portaria().isEmpty()) {
            List<String> doPorteiro = new ArrayList<>();
            if (caminhoUnidade != null && !escopo.unidadesProprias().isEmpty()) {
                doPorteiro.add(caminhoUnidade + " in :unidadesProprias");
                parametros.put("unidadesProprias", escopo.unidadesProprias());
            }
            if (caminhoAutor != null) {
                doPorteiro.add(caminhoAutor + " = :pessoa");
                parametros.put("pessoa", escopo.pessoa());
            }
            if (!doPorteiro.isEmpty()) {
                alternativas.add("(" + caminhoCondominio + " in :condominiosDaPortaria and ("
                        + String.join(" or ", doPorteiro) + "))");
                parametros.put("condominiosDaPortaria", escopo.portaria());
            }
        }
        return alternativas.isEmpty() ? "1 = 0" : "(" + String.join(" or ", alternativas) + ")";
    }

    /**
     * Condomínios que entram no painel: {@code todos} para o administrador geral sem filtro; senão, os da
     * administração (tudo conta) e os da portaria (só o que é da própria pessoa).
     */
    private record Escopo(boolean todos, Set<Integer> gestao, Set<Integer> portaria, Set<Integer> unidadesProprias,
                          Integer pessoa) {

        Escopo somenteGestao() {
            return new Escopo(todos, gestao, Set.of(), Set.of(), pessoa);
        }

        boolean vazio() {
            return !todos && gestao.isEmpty() && portaria.isEmpty();
        }
    }
}
