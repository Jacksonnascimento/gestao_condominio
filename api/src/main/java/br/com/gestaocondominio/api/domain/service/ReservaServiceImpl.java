package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ReservaConvidadoDTO;
import br.com.gestaocondominio.api.controller.dto.ReservaRequestDTO;
import br.com.gestaocondominio.api.controller.v1.dto.AreaComumDTOs.AreaComumResposta;
import br.com.gestaocondominio.api.controller.v1.dto.AreaComumDTOs.CondominioOpcao;
import br.com.gestaocondominio.api.controller.v1.dto.Opcao;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs.AreaOpcao;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs.OpcoesReserva;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs.ReservaResposta;
import br.com.gestaocondominio.api.controller.v1.dto.ReservaDTOs.UnidadeOpcao;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.ReservaStatus;
import br.com.gestaocondominio.api.domain.repository.AreaComumRepository;
import br.com.gestaocondominio.api.domain.repository.AreaComumTurnoRepository;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.ReservaRepository;
import br.com.gestaocondominio.api.domain.repository.ReservaSpecification;
import br.com.gestaocondominio.api.exception.ConflitoException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final AreaComumService areaComumService;
    private final AreaComumTurnoRepository turnoRepository;
    private final AreaComumRepository areaComumRepository;
    private final OcupanteRepository ocupanteRepository;
    private final CondominioService condominioService;

    @Override
    @Transactional(readOnly = true)
    public Page<ReservaResposta> consultarReservas(Pessoa usuario, Integer conCod, ReservaStatus status, String busca,
                                                   Integer areCod, LocalDate dataInicio, LocalDate dataFim,
                                                   Pageable pageable) {
        Specification<Reserva> spec = visiveisPara(usuario)
                .and(ReservaSpecification.filtrar(conCod, status, busca, areCod, dataInicio, dataFim));
        Page<Reserva> pagina = reservaRepository.findAll(spec, pageable);
        if (pagina.hasContent()) {
            // Preenche os convidados das reservas já carregadas, em vez de uma consulta por reserva
            reservaRepository.findByResCodIn(pagina.getContent().stream().map(Reserva::getResCod).toList());
        }
        Set<Integer> geridos = areaComumService.condominiosGerenciados(usuario);
        return pagina.map(reserva -> paraResposta(reserva, usuario, geridos));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> contarReservas(Pessoa usuario, Integer conCod) {
        Specification<Reserva> spec = visiveisPara(usuario)
                .and(ReservaSpecification.filtrar(conCod, null, null, null, null, null));
        return Map.of(
                "TOTAL", reservaRepository.count(spec),
                "PENDENTES", reservaRepository.count(spec.and(ReservaSpecification.filtrar(
                        null, ReservaStatus.PENDENTE_APROVACAO, null, null, null, null))),
                "APROVADAS", reservaRepository.count(spec.and(ReservaSpecification.filtrar(
                        null, ReservaStatus.APROVADA, null, null, null, null))));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResposta buscarPorIdDTO(Integer resCod, Pessoa usuario) {
        Reserva reserva = carregar(resCod);
        boolean solicitante = reserva.getMorador().getPesCod().equals(usuario.getPesCod());
        if (!solicitante && !areaComumService.podeGerenciar(usuario, condominioDa(reserva))) {
            throw new AccessDeniedException("Você não pode ver esta reserva.");
        }
        return paraResposta(reserva, usuario, areaComumService.condominiosGerenciados(usuario));
    }

    @Override
    @Transactional
    public Reserva solicitarReserva(ReservaRequestDTO dto, Pessoa usuario) {
        // Só para uma unidade em que a pessoa mora. A regra antiga também aceitava o síndico e a administração para
        // qualquer unidade do condomínio, mas a tela nunca mostrou a solicitação para eles.
        Unidade unidade = unidadesDe(usuario).stream()
                .filter(u -> u.getUniCod().equals(dto.getUniCod()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você só pode solicitar reservas para uma unidade em que mora."));

        AreaComum area = areaComumRepository.findById(dto.getAreCod())
                .orElseThrow(() -> new EntityNotFoundException("Área comum não encontrada."));
        if (!area.getCondominio().getConCod().equals(unidade.getCondominio().getConCod())) {
            throw new IllegalArgumentException("Esta área comum não é do condomínio da unidade escolhida.");
        }
        if (!Boolean.TRUE.equals(area.getAtiva())) {
            throw new IllegalArgumentException("Esta área comum não está disponível para reservas.");
        }
        if (dto.getTurCod() != null) {
            AreaComumTurno turno = area.getTurnos().stream()
                    .filter(t -> t.getTurCod().equals(dto.getTurCod()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("O turno escolhido não é desta área comum."));
            if (Boolean.FALSE.equals(turno.getAtivo())) {
                throw new IllegalArgumentException("Este turno não está disponível para reservas.");
            }
        }
        // A regra antiga descartava os convidados sem avisar; pela API a pessoa fica sabendo
        if (dto.getConvidados() != null && !dto.getConvidados().isEmpty()
                && !Boolean.TRUE.equals(area.getPermiteConvidados())) {
            throw new IllegalArgumentException("Esta área comum não permite convidados.");
        }

        return gravarSolicitacao(dto, area, unidade, usuario);
    }

    @Override
    @Transactional
    public Reserva aprovarReserva(Integer resCod, Pessoa usuario) {
        Reserva reserva = carregar(resCod);
        exigirGestao(reserva, usuario);
        if (reserva.getStatus() != ReservaStatus.PENDENTE_APROVACAO) {
            throw new IllegalArgumentException("Apenas reservas pendentes podem ser aprovadas.");
        }
        reserva.setStatus(ReservaStatus.APROVADA);
        reserva.setAprovador(usuario);
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public Reserva rejeitarReserva(Integer resCod, Pessoa usuario, String motivo) {
        Reserva reserva = carregar(resCod);
        exigirGestao(reserva, usuario);
        if (reserva.getStatus() != ReservaStatus.PENDENTE_APROVACAO) {
            throw new IllegalArgumentException("Apenas reservas pendentes podem ser rejeitadas.");
        }
        reserva.setStatus(ReservaStatus.REJEITADA);
        reserva.setAprovador(usuario);
        reserva.setMotivoRejeicao(motivo.trim());
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public Reserva cancelarReserva(Integer resCod, Pessoa usuario) {
        Reserva reserva = carregar(resCod);
        if (!reserva.getMorador().getPesCod().equals(usuario.getPesCod())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas quem solicitou a reserva pode cancelá-la.");
        }
        // Como na tela: o botão de cancelar só aparece para reservas pendentes ou aprovadas
        if (!podeSerCancelada(reserva)) {
            throw new IllegalArgumentException("Só é possível cancelar reservas pendentes ou aprovadas.");
        }
        reserva.setStatus(ReservaStatus.CANCELADA_PELO_MORADOR);
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public OpcoesReserva opcoes(Pessoa usuario, Integer conCod) {
        boolean administradorGeral = Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin());
        Set<Integer> geridos = areaComumService.condominiosGerenciados(usuario);

        List<Unidade> unidades = unidadesDe(usuario).stream()
                .filter(u -> conCod == null || u.getCondominio().getConCod().equals(conCod))
                .toList();
        Set<Integer> condominiosDasUnidades = unidades.stream()
                .map(u -> u.getCondominio().getConCod())
                .collect(Collectors.toSet());

        List<AreaComumResposta> areasParaSolicitar = areasDos(condominiosDasUnidades).stream()
                .filter(area -> Boolean.TRUE.equals(area.getAtiva()))
                .map(AreaComumResposta::paraReserva)
                .toList();

        // Filtro por área: as dos condomínios que a pessoa gerencia e as dos condomínios em que ela mora
        List<AreaComum> areasDoFiltro;
        if (administradorGeral) {
            areasDoFiltro = conCod != null
                    ? areaComumRepository.findByCondominioConCodOrderByNomeAsc(conCod)
                    : areaComumRepository.findAllByOrderByNomeAsc();
        } else {
            Set<Integer> condominios = new HashSet<>(geridos);
            condominios.addAll(condominiosDasUnidades);
            if (conCod != null) {
                condominios.retainAll(Set.of(conCod));
            }
            areasDoFiltro = areasDos(condominios);
        }

        return new OpcoesReserva(
                Arrays.stream(ReservaStatus.values()).map(s -> new Opcao(s.name(), ReservaDTOs.descricaoDo(s))).toList(),
                condominioService.listarTodosCondominios(false).stream().map(CondominioOpcao::de).toList(),
                unidades.stream().map(UnidadeOpcao::de).toList(),
                areasParaSolicitar,
                areasDoFiltro.stream()
                        .map(area -> new AreaOpcao(area.getAreCod(), area.getNome(), area.getCondominio().getConCod()))
                        .toList(),
                administradorGeral || !geridos.isEmpty(),
                !unidades.isEmpty());
    }

    /** Grava a solicitação já conferida quanto à unidade, à área e ao turno: faltam antecedência e disponibilidade. */
    private Reserva gravarSolicitacao(ReservaRequestDTO dto, AreaComum area, Unidade unidade, Pessoa morador) {
        if (dto.getTermosAceitos() == null || !dto.getTermosAceitos()) {
            throw new IllegalArgumentException("É obrigatório aceitar os termos de uso.");
        }

        validarAntecedencia(area, dto.getData());
        validarDisponibilidade(area, dto.getTurCod(), dto.getData());

        Reserva reserva = new Reserva();
        reserva.setAreaComum(area);
        reserva.setUnidade(unidade);
        reserva.setMorador(morador);
        reserva.setData(dto.getData());
        reserva.setTermosAceitos(dto.getTermosAceitos());
        reserva.setStatus(ReservaStatus.PENDENTE_APROVACAO);

        if (dto.getTurCod() != null) {
            AreaComumTurno turno = turnoRepository.findById(dto.getTurCod())
                    .orElseThrow(() -> new EntityNotFoundException("Turno não encontrado."));
            reserva.setTurno(turno);
        }

        reserva.setConvidados(new ArrayList<>());
        if (Boolean.TRUE.equals(area.getPermiteConvidados()) && dto.getConvidados() != null) {
            if (area.getLimiteConvidados() != null && dto.getConvidados().size() > area.getLimiteConvidados()) {
                throw new IllegalArgumentException("Esta área comum permite no máximo " + area.getLimiteConvidados()
                        + " convidado(s).");
            }

            for (ReservaConvidadoDTO convDTO : dto.getConvidados()) {
                ReservaConvidado convidado = ReservaConvidado.builder()
                        .reserva(reserva)
                        .nome(convDTO.getNome())
                        .documento(convDTO.getDocumento())
                        .build();
                reserva.getConvidados().add(convidado);
            }
        }

        return reservaRepository.save(reserva);
    }

    private Specification<Reserva> visiveisPara(Pessoa usuario) {
        if (Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin())) {
            return (root, query, cb) -> cb.conjunction();
        }
        return ReservaSpecification.visiveisPara(usuario.getPesCod(), areaComumService.condominiosGerenciados(usuario));
    }

    private ReservaResposta paraResposta(Reserva reserva, Pessoa usuario, Set<Integer> geridos) {
        boolean gestor = Boolean.TRUE.equals(usuario.getPesIsGlobalAdmin()) || geridos.contains(condominioDa(reserva));
        boolean solicitante = reserva.getMorador().getPesCod().equals(usuario.getPesCod());
        return ReservaResposta.de(reserva,
                gestor && reserva.getStatus() == ReservaStatus.PENDENTE_APROVACAO,
                solicitante && podeSerCancelada(reserva));
    }

    private static boolean podeSerCancelada(Reserva reserva) {
        return reserva.getStatus() == ReservaStatus.PENDENTE_APROVACAO || reserva.getStatus() == ReservaStatus.APROVADA;
    }

    private void exigirGestao(Reserva reserva, Pessoa usuario) {
        if (!areaComumService.podeGerenciar(usuario, condominioDa(reserva))) {
            throw new AccessDeniedException("Você não gerencia as reservas deste condomínio.");
        }
    }

    private Reserva carregar(Integer resCod) {
        return reservaRepository.findById(resCod)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada."));
    }

    private static Integer condominioDa(Reserva reserva) {
        return reserva.getAreaComum().getCondominio().getConCod();
    }

    /** Unidades em que a pessoa está cadastrada como ocupante. */
    private List<Unidade> unidadesDe(Pessoa usuario) {
        return ocupanteRepository.findByPessoa(usuario).stream()
                .map(Ocupante::getUnidade)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<AreaComum> areasDos(Set<Integer> condominios) {
        return condominios.isEmpty() ? List.of() : areaComumRepository.findByCondominioConCodInOrderByNomeAsc(condominios);
    }

    private void validarAntecedencia(AreaComum area, LocalDate dataReserva) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), dataReserva);
        if (dias < area.getDiasAntecedenciaMin()) {
            throw new IllegalArgumentException("A reserva deve ser feita com no mínimo " + area.getDiasAntecedenciaMin() + " dia(s) de antecedência.");
        }
        if (dias > area.getDiasAntecedenciaMax()) {
            throw new IllegalArgumentException("A reserva não pode ultrapassar " + area.getDiasAntecedenciaMax() + " dia(s) de antecedência.");
        }
    }

    /**
     * Conflito na mesma área e data com reserva que não foi cancelada nem rejeitada. Reserva do dia inteiro (sem
     * turno) conflita com qualquer outra da data; reserva de um turno conflita com a do mesmo turno e com a do dia
     * inteiro (antes a do dia inteiro não bloqueava os turnos).
     */
    private void validarDisponibilidade(AreaComum area, Integer turCod, LocalDate data) {
        List<Reserva> conflitantes = new ArrayList<>(reservaRepository.findByAreaComumAreCodAndDataAndStatusNot(
                area.getAreCod(), data, ReservaStatus.CANCELADA_PELO_MORADOR));

        conflitantes.removeIf(r -> r.getStatus() == ReservaStatus.REJEITADA);
        if (turCod != null) {
            conflitantes.removeIf(r -> r.getTurno() != null && !turCod.equals(r.getTurno().getTurCod()));
        }

        if (!conflitantes.isEmpty()) {
            throw new ConflitoException("Já existe uma reserva para esta área e turno nesta data.");
        }
    }
}
