package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ReservaRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Reserva;

import java.util.List;

public interface ReservaService {
    Reserva solicitarReserva(ReservaRequestDTO dto);
    Reserva aprovarReserva(Integer resCod, Integer pesCodAprovador);
    Reserva rejeitarReserva(Integer resCod, Integer pesCodAprovador, String motivo);
    Reserva cancelarReserva(Integer resCod, Integer pesCodMorador);
    Reserva buscarPorId(Integer resCod);
    List<Reserva> listarPorCondominio(Integer conCod);
    List<Reserva> listarPorMorador(Integer pesCod);
}