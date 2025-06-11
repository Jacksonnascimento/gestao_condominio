package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.ReservaAreaComum;
import br.com.gestaocondominio.api.domain.entity.AreaComum;
import br.com.gestaocondominio.api.domain.repository.ReservaAreaComumRepository;
import br.com.gestaocondominio.api.domain.repository.AreaComumRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservaAreaComumService {

    private final ReservaAreaComumRepository reservaAreaComumRepository;
    private final AreaComumRepository areaComumRepository;
    private final UnidadeRepository unidadeRepository;
    private final PessoaRepository pessoaRepository;

    public ReservaAreaComumService(ReservaAreaComumRepository reservaAreaComumRepository,
                                   AreaComumRepository areaComumRepository,
                                   UnidadeRepository unidadeRepository,
                                   PessoaRepository pessoaRepository) {
        this.reservaAreaComumRepository = reservaAreaComumRepository;
        this.areaComumRepository = areaComumRepository;
        this.unidadeRepository = unidadeRepository;
        this.pessoaRepository = pessoaRepository;
    }

    public ReservaAreaComum cadastrarReservaAreaComum(ReservaAreaComum reserva) {
        if (reserva.getAreaComum() == null || reserva.getAreaComum().getArcCod() == null) {
            throw new IllegalArgumentException("Área Comum deve ser informada para a reserva.");
        }
        AreaComum areaComum = areaComumRepository.findById(reserva.getAreaComum().getArcCod())
                .orElseThrow(() -> new IllegalArgumentException("Área Comum não encontrada com o ID: " + reserva.getAreaComum().getArcCod()));

        if (reserva.getUnidade() == null || reserva.getUnidade().getUniCod() == null) {
            throw new IllegalArgumentException("Unidade deve ser informada para a reserva.");
        }
        unidadeRepository.findById(reserva.getUnidade().getUniCod())
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + reserva.getUnidade().getUniCod()));

        if (reserva.getSolicitante() == null || reserva.getSolicitante().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa solicitante deve ser informada para a reserva.");
        }
        pessoaRepository.findById(reserva.getSolicitante().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa solicitante não encontrada com o ID: " + reserva.getSolicitante().getPesCod()));

        if (reserva.getDataHoraInicio() == null || reserva.getDataHoraFim() == null) {
            throw new IllegalArgumentException("Data e hora de início e fim da reserva devem ser informadas.");
        }
        if (reserva.getDataHoraInicio().isAfter(reserva.getDataHoraFim())) {
            throw new IllegalArgumentException("A data/hora de início da reserva não pode ser posterior à data/hora de fim.");
        }

        if (!areaComum.getArcPermiteReserva()) {
            throw new IllegalArgumentException("Esta Área Comum não permite reservas.");
        }

        List<ReservaAreaComum> conflitos = reservaAreaComumRepository.findByAreaComumAndDataHoraFimAfterAndDataHoraInicioBefore(
            areaComum, reserva.getDataHoraInicio(), reserva.getDataHoraFim()
        );
        if (!conflitos.isEmpty()) {
            throw new IllegalArgumentException("Já existe uma reserva para esta área comum neste período. Conflito com a reserva ID: " + conflitos.get(0).getRacCod());
        }

        if (reserva.getStatus() == null || reserva.getStatus().trim().isEmpty()) {
            reserva.setStatus("SOLICITADA");
        }

        reserva.setDtSolicitacao(LocalDateTime.now());
        reserva.setDtAtualizacao(LocalDateTime.now());

        return reservaAreaComumRepository.save(reserva);
    }

    public Optional<ReservaAreaComum> buscarReservaAreaComumPorId(Integer id) {
        return reservaAreaComumRepository.findById(id);
    }

    public List<ReservaAreaComum> listarTodasReservasAreaComum() {
        return reservaAreaComumRepository.findAll();
    }

    public ReservaAreaComum atualizarReservaAreaComum(Integer id, ReservaAreaComum reservaAtualizada) {
        ReservaAreaComum reservaExistente = reservaAreaComumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva de área comum não encontrada com o ID: " + id));

        if (reservaAtualizada.getAreaComum() != null && !reservaAtualizada.getAreaComum().getArcCod().equals(reservaExistente.getAreaComum().getArcCod())) {
             throw new IllegalArgumentException("Não é permitido alterar a Área Comum de uma reserva existente.");
        }
        if (reservaAtualizada.getUnidade() != null && !reservaAtualizada.getUnidade().getUniCod().equals(reservaExistente.getUnidade().getUniCod())) {
             throw new IllegalArgumentException("Não é permitido alterar a Unidade de uma reserva existente.");
        }
        if (reservaAtualizada.getSolicitante() != null && !reservaAtualizada.getSolicitante().getPesCod().equals(reservaExistente.getSolicitante().getPesCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Solicitante de uma reserva existente.");
        }

        if (reservaAtualizada.getDataHoraInicio() == null || reservaAtualizada.getDataHoraFim() == null) {
            throw new IllegalArgumentException("Data e hora de início e fim da reserva devem ser informadas na atualização.");
        }
        if (reservaAtualizada.getDataHoraInicio().isAfter(reservaAtualizada.getDataHoraFim())) {
            throw new IllegalArgumentException("A data/hora de início da reserva não pode ser posterior à data/hora de fim na atualização.");
        }
        
        List<ReservaAreaComum> conflitosNaAtualizacao = reservaAreaComumRepository.findByAreaComumAndDataHoraFimAfterAndDataHoraInicioBeforeAndRacCodIsNot(
            reservaExistente.getAreaComum(),
            reservaAtualizada.getDataHoraInicio(),
            reservaAtualizada.getDataHoraFim(),
            id
        );
        if (!conflitosNaAtualizacao.isEmpty()) {
            throw new IllegalArgumentException("A atualização da reserva causa conflito de horário com outra reserva. Conflito com a reserva ID: " + conflitosNaAtualizacao.get(0).getRacCod());
        }

        if (reservaAtualizada.getDataHoraInicio() != null) {
            reservaExistente.setDataHoraInicio(reservaAtualizada.getDataHoraInicio());
        }
        if (reservaAtualizada.getDataHoraFim() != null) {
            reservaExistente.setDataHoraFim(reservaAtualizada.getDataHoraFim());
        }
        if (reservaAtualizada.getStatus() != null) {
            reservaExistente.setStatus(reservaAtualizada.getStatus());
        }
        if (reservaAtualizada.getObservacoes() != null) {
            reservaExistente.setObservacoes(reservaAtualizada.getObservacoes());
        }
        
        reservaExistente.setDtAtualizacao(LocalDateTime.now());
        return reservaAreaComumRepository.save(reservaExistente);
    }

    
    public void deletarReservaAreaComum(Integer id) {
        reservaAreaComumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva de área comum não encontrada para exclusão com o ID: " + id));
        reservaAreaComumRepository.deleteById(id);
    }
}