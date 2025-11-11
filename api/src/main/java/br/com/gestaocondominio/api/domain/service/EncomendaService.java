package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.EncomendaDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaRequestDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaRetiradaRequestDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaStatusRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Encomenda;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.EncomendaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface EncomendaService {

    Page<EncomendaDTO> consultarEncomendas(Pessoa usuarioLogado, Integer condominioId, String busca, Integer unidadeId, EncomendaStatus status, Pageable pageable);

    Map<String, Long> contarStatusEncomendas(Pessoa usuarioLogado, Integer condominioId, String busca, Integer unidadeId, EncomendaStatus status);

    Encomenda criarEncomenda(EncomendaRequestDTO dto, Pessoa usuarioLogado);

    Encomenda registrarRetirada(Long encomendaId, EncomendaRetiradaRequestDTO dto, Pessoa usuarioLogado);

    Encomenda atualizarStatus(Long encomendaId, EncomendaStatusRequestDTO dto, Pessoa usuarioLogado);

    Encomenda buscarPorIdEValidarAcesso(Long id, Pessoa usuarioLogado, boolean paraEscrita);

    
    EncomendaDTO buscarPorIdDTO(Long id, Pessoa usuarioLogado);
}