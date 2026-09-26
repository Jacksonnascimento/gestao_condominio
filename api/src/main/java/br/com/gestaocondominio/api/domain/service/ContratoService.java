package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ContratoRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Contrato;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.StatusContrato;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.ContratoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContratoService {

    /** Papéis que veem e gerenciam os contratos do condomínio. */
    public static final UserRole[] PAPEIS_DE_GESTAO = {UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM};

    @Autowired
    private ContratoRepository contratoRepository;
    @Autowired
    private CondominioRepository condominioRepository;
    @Autowired
    private UsuarioCondominioService usuarioCondominioService;

    /** Contratos de um conjunto de condomínios; {@code null} não filtra por condomínio. */
    private List<Contrato> listarContratos(Collection<Integer> condominioIds, String busca, StatusContrato status,
            Boolean isProximoVencimento, Boolean isHistorico, LocalDate inicioApos, LocalDate fimAntes) {
        Specification<Contrato> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (condominioIds != null) {
                predicates.add(root.get("condominio").get("conCod").in(condominioIds));
            }

            if (StringUtils.hasText(busca)) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("empresa")), "%" + busca.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("servico")), "%" + busca.toLowerCase() + "%")));
            }

            if (inicioApos != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataInicio"), inicioApos));
            }

            if (fimAntes != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataFim"), fimAntes));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Contrato> todosContratos = contratoRepository.findAll(spec);
        todosContratos.forEach(this::atualizarStatusCalculado);

        todosContratos.sort(Comparator.comparing(Contrato::getDataInicio)
                .thenComparing(Contrato::getDataFim));

        if (Boolean.TRUE.equals(isHistorico)) {
            return todosContratos.stream()
                    .filter(c -> c.getStatus() == StatusContrato.FINALIZADO
                            || c.getStatus() == StatusContrato.RESCINDIDO)
                    .filter(c -> status == null || c.getStatus() == status)
                    .collect(Collectors.toList());
        } else if (Boolean.TRUE.equals(isProximoVencimento)) {
            return todosContratos.stream()
                    .filter(c -> c.getStatus() == StatusContrato.A_VENCER)
                    .collect(Collectors.toList());
        } else {
            return todosContratos.stream()
                    .filter(c -> c.getStatus() == StatusContrato.ATIVO)
                    .collect(Collectors.toList());
        }
    }

    private Map<StatusContrato, Long> contarContratosPorStatus(Collection<Integer> condominioIds) {
        Specification<Contrato> spec = (root, query, cb) -> {
            if (condominioIds == null) {
                return cb.conjunction();
            }
            return root.get("condominio").get("conCod").in(condominioIds);
        };

        List<Contrato> todosContratos = contratoRepository.findAll(spec);
        todosContratos.forEach(this::atualizarStatusCalculado);

        return todosContratos.stream()
                .collect(Collectors.groupingBy(Contrato::getStatus, Collectors.counting()));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Operações conferindo quem está logado. Contratos são vistos e gerenciados pelo administrador
    // geral, em todos os condomínios, e por síndico, administração e funcionário administrativo, só nos condomínios em
    // que têm esse papel.
    // ---------------------------------------------------------------------------------------------------------------

    public boolean podeGerenciar(Pessoa usuario) {
        return UsuarioCondominioService.isAdministradorGeral(usuario)
                || !usuarioCondominioService.condominiosComPapel(usuario, PAPEIS_DE_GESTAO).isEmpty();
    }

    /** Condomínios cujos contratos a pessoa gerencia, para a escolha na listagem e no cadastro. */
    public List<Condominio> condominiosDisponiveis(Pessoa usuario) {
        return usuarioCondominioService.condominiosDisponiveis(usuario, PAPEIS_DE_GESTAO);
    }

    /**
     * Contratos de uma das abas da tela (ativos, próximos a vencer ou histórico), dos condomínios que a pessoa
     * gerencia, ordenados por início e fim. O {@code status} só filtra o histórico (finalizados ou rescindidos).
     */
    public Page<Contrato> consultarContratos(Pessoa usuario, Integer condominioId, String busca, StatusContrato status,
                                             boolean proximosAVencer, boolean historico, LocalDate inicioApos,
                                             LocalDate fimAntes, Pageable pageable) {
        List<Contrato> contratos = listarContratos(alcance(usuario, condominioId), busca, historico ? status : null,
                proximosAVencer, historico, inicioApos, fimAntes);
        int inicio = (int) Math.min(pageable.getOffset(), contratos.size());
        int fim = Math.min(inicio + pageable.getPageSize(), contratos.size());
        return new PageImpl<>(contratos.subList(inicio, fim), pageable, contratos.size());
    }

    public Map<StatusContrato, Long> contarContratosPorStatus(Pessoa usuario, Integer condominioId) {
        return contarContratosPorStatus(alcance(usuario, condominioId));
    }

    /** Um contrato, com a situação calculada pela data de fim, se a pessoa gerencia o condomínio dele. */
    public Contrato buscarContrato(Long id, Pessoa usuario) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contrato não encontrado."));
        conferirGestao(usuario, contrato.getCondominio().getConCod());
        atualizarStatusCalculado(contrato);
        return contrato;
    }

    /**
     * Cadastra o contrato no condomínio informado. Sem condomínio, vale o único que a pessoa gerencia, como na tela
     * antiga, em que só o administrador geral escolhe.
     */
    public Contrato criarContrato(ContratoRequestDTO dto, Pessoa usuario) {
        Integer condominioId = dto.getCondominioId() != null ? dto.getCondominioId() : unicoCondominio(usuario);
        conferirGestao(usuario, condominioId);
        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado."));
        validarPeriodo(dto);
        Contrato contrato = new Contrato();
        contrato.setCondominio(condominio);
        preencherDadosContrato(contrato, dto);
        return contratoRepository.save(contrato);
    }

    /** Altera os dados do contrato. O condomínio dele não muda. */
    public Contrato atualizarContrato(Long id, ContratoRequestDTO dto, Pessoa usuario) {
        Contrato contrato = buscarContrato(id, usuario);
        validarPeriodo(dto);
        preencherDadosContrato(contrato, dto);
        return contratoRepository.save(contrato);
    }

    public void excluirContrato(Long id, Pessoa usuario) {
        contratoRepository.delete(buscarContrato(id, usuario));
    }

    /** Condomínios da consulta: o pedido, se a pessoa o gerencia; senão, todos os que ela gerencia. */
    private Set<Integer> alcance(Pessoa usuario, Integer condominioId) {
        if (condominioId != null) {
            conferirGestao(usuario, condominioId);
            return Set.of(condominioId);
        }
        if (UsuarioCondominioService.isAdministradorGeral(usuario)) {
            return null;
        }
        Set<Integer> condominios = usuarioCondominioService.condominiosComPapel(usuario, PAPEIS_DE_GESTAO);
        if (condominios.isEmpty()) {
            throw new AccessDeniedException("Você não gerencia contratos de nenhum condomínio.");
        }
        return condominios;
    }

    private void conferirGestao(Pessoa usuario, Integer condominioId) {
        if (!usuarioCondominioService.possuiPapelNoCondominio(usuario, condominioId, PAPEIS_DE_GESTAO)) {
            throw new AccessDeniedException("Você não gerencia os contratos deste condomínio.");
        }
    }

    private Integer unicoCondominio(Pessoa usuario) {
        if (!UsuarioCondominioService.isAdministradorGeral(usuario)) {
            Set<Integer> condominios = usuarioCondominioService.condominiosComPapel(usuario, PAPEIS_DE_GESTAO);
            if (condominios.size() == 1) {
                return condominios.iterator().next();
            }
        }
        throw new IllegalArgumentException("Informe o condomínio.");
    }

    private void validarPeriodo(ContratoRequestDTO dto) {
        if (dto.getDataInicio() != null && dto.getDataFim() != null && dto.getDataFim().isBefore(dto.getDataInicio())) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início.");
        }
    }

    private void atualizarStatusCalculado(Contrato contrato) {

        if (contrato.getStatus() != StatusContrato.RESCINDIDO) {
            contrato.setStatus(calcularStatus(contrato.getDataFim()));
        }
    }

    private void preencherDadosContrato(Contrato contrato, ContratoRequestDTO dto) {
        contrato.setEmpresa(dto.getEmpresa());
        contrato.setServico(dto.getServico());
        contrato.setValor(dto.getValor());
        contrato.setResponsavel(dto.getResponsavel());
        contrato.setDataInicio(dto.getDataInicio());
        contrato.setDataFim(dto.getDataFim());
        contrato.setObservacoes(dto.getObservacoes());

        if (dto.getStatus() == StatusContrato.RESCINDIDO) {
            contrato.setStatus(dto.getStatus());
        } else {

            contrato.setStatus(calcularStatus(dto.getDataFim()));
        }
    }

    private StatusContrato calcularStatus(LocalDate dataFim) {
        LocalDate hoje = LocalDate.now();
        if (dataFim == null)
            return StatusContrato.ATIVO;
        if (dataFim.isBefore(hoje))
            return StatusContrato.FINALIZADO;
        if (dataFim.isBefore(hoje.plusDays(30)))
            return StatusContrato.A_VENCER;
        return StatusContrato.ATIVO;
    }
}