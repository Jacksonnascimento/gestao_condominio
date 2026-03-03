package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ReservaConvidadoDTO;
import br.com.gestaocondominio.api.controller.dto.ReservaRequestDTO;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.ReservaStatus;
import br.com.gestaocondominio.api.domain.repository.AreaComumTurnoRepository;
import br.com.gestaocondominio.api.domain.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final AreaComumService areaComumService;
    private final UnidadeService unidadeService;
    private final PessoaService pessoaService;
    private final AreaComumTurnoRepository turnoRepository;

    @Override
    @Transactional
    public Reserva solicitarReserva(ReservaRequestDTO dto) {
        if (dto.getTermosAceitos() == null || !dto.getTermosAceitos()) {
            throw new RuntimeException("É obrigatório aceitar os termos de uso.");
        }

        AreaComum area = areaComumService.buscarPorId(dto.getAreCod());
        
        Unidade unidade = unidadeService.buscarUnidadePorId(dto.getUniCod())
                .orElseThrow(() -> new RuntimeException("Unidade não encontrada."));
                
        Pessoa morador = pessoaService.buscarPessoaPorId(dto.getPesCodMorador())
                .orElseThrow(() -> new RuntimeException("Morador não encontrado."));

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
                    .orElseThrow(() -> new RuntimeException("Turno não encontrado."));
            reserva.setTurno(turno);
        }

        reserva.setConvidados(new ArrayList<>());
        if (Boolean.TRUE.equals(area.getPermiteConvidados()) && dto.getConvidados() != null) {
            if (area.getLimiteConvidados() != null && dto.getConvidados().size() > area.getLimiteConvidados()) {
                throw new RuntimeException("Limite de convidados excedido. Máximo permitido: " + area.getLimiteConvidados());
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

    @Override
    @Transactional
    public Reserva aprovarReserva(Integer resCod, Integer pesCodAprovador) {
        Reserva reserva = buscarPorId(resCod);
        if (reserva.getStatus() != ReservaStatus.PENDENTE_APROVACAO) {
            throw new RuntimeException("Apenas reservas pendentes podem ser aprovadas.");
        }
        reserva.setStatus(ReservaStatus.APROVADA);
        reserva.setAprovador(pessoaService.buscarPessoaPorId(pesCodAprovador)
                .orElseThrow(() -> new RuntimeException("Aprovador não encontrado.")));
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public Reserva rejeitarReserva(Integer resCod, Integer pesCodAprovador, String motivo) {
        Reserva reserva = buscarPorId(resCod);
        if (reserva.getStatus() != ReservaStatus.PENDENTE_APROVACAO) {
            throw new RuntimeException("Apenas reservas pendentes podem ser rejeitadas.");
        }
        reserva.setStatus(ReservaStatus.REJEITADA);
        reserva.setAprovador(pessoaService.buscarPessoaPorId(pesCodAprovador)
                .orElseThrow(() -> new RuntimeException("Aprovador não encontrado.")));
        reserva.setMotivoRejeicao(motivo);
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public Reserva cancelarReserva(Integer resCod, Integer pesCodMorador) {
        Reserva reserva = buscarPorId(resCod);
        if (!reserva.getMorador().getPesCod().equals(pesCodMorador)) {
            throw new RuntimeException("Apenas o solicitante pode cancelar esta reserva.");
        }
        if (reserva.getStatus() == ReservaStatus.CONCLUIDA || reserva.getStatus() == ReservaStatus.REJEITADA) {
            throw new RuntimeException("Não é possível cancelar uma reserva neste status.");
        }
        reserva.setStatus(ReservaStatus.CANCELADA_PELO_MORADOR);
        return reservaRepository.save(reserva);
    }

    @Override
    public Reserva buscarPorId(Integer resCod) {
        return reservaRepository.findById(resCod)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada."));
    }

    @Override
    public List<Reserva> listarPorCondominio(Integer conCod) {
        return reservaRepository.findByAreaComumCondominioConCodOrderByDataDesc(conCod);
    }

    @Override
    public List<Reserva> listarPorMorador(Integer pesCod) {
        return reservaRepository.findByMoradorPesCodOrderByDataDesc(pesCod);
    }

    private void validarAntecedencia(AreaComum area, LocalDate dataReserva) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), dataReserva);
        if (dias < area.getDiasAntecedenciaMin()) {
            throw new RuntimeException("A reserva deve ser feita com no mínimo " + area.getDiasAntecedenciaMin() + " dia(s) de antecedência.");
        }
        if (dias > area.getDiasAntecedenciaMax()) {
            throw new RuntimeException("A reserva não pode ultrapassar " + area.getDiasAntecedenciaMax() + " dia(s) de antecedência.");
        }
    }

    private void validarDisponibilidade(AreaComum area, Integer turCod, LocalDate data) {
        List<Reserva> conflitantes;
        if (turCod != null) {
            conflitantes = reservaRepository.findByAreaComumAreCodAndTurnoTurCodAndDataAndStatusNot(
                    area.getAreCod(), turCod, data, ReservaStatus.CANCELADA_PELO_MORADOR);
        } else {
            conflitantes = reservaRepository.findByAreaComumAreCodAndDataAndStatusNot(
                    area.getAreCod(), data, ReservaStatus.CANCELADA_PELO_MORADOR);
        }

        conflitantes.removeIf(r -> r.getStatus() == ReservaStatus.REJEITADA);

        if (!conflitantes.isEmpty()) {
            throw new RuntimeException("Já existe uma reserva para esta área/turno nesta data.");
        }
    }
}