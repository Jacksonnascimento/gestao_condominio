package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.ContratoRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Contrato;
import br.com.gestaocondominio.api.domain.enums.StatusContrato;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.ContratoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContratoService {

    @Autowired
    private ContratoRepository contratoRepository;
    @Autowired
    private CondominioRepository condominioRepository;

    public Contrato criarContrato(Integer condominioId, ContratoRequestDTO dto) {
        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new RuntimeException("Condomínio não encontrado"));

        Contrato contrato = new Contrato();
        contrato.setCondominio(condominio);
        preencherDadosContrato(contrato, dto);
        return contratoRepository.save(contrato);
    }

    public Contrato obterPorId(Long id) {
        return contratoRepository.findById(id).orElseThrow(() -> new RuntimeException("Contrato não encontrado"));
    }

    public Contrato atualizarContrato(Long id, ContratoRequestDTO dto) {
        Contrato contrato = obterPorId(id);
        preencherDadosContrato(contrato, dto);
        return contratoRepository.save(contrato);
    }

    public void deletarContrato(Long id) {
        contratoRepository.deleteById(id);
    }

    public List<Contrato> listarContratos(Integer condominioId, String busca, StatusContrato status, LocalDate dataInicio, LocalDate dataFim) {
        Specification<Contrato> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (condominioId != null) {
                predicates.add(cb.equal(root.get("condominio").get("conCod"), condominioId));
            }

            if (busca != null && !busca.isEmpty()) {
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("empresa")), "%" + busca.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("servico")), "%" + busca.toLowerCase() + "%")
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return contratoRepository.findAll(spec);
    }

    public Map<StatusContrato, Long> contarContratosPorStatus(Integer condominioId) {
        Specification<Contrato> spec = (root, query, cb) -> {
            if (condominioId == null) {
                return cb.conjunction(); // Retorna uma condição "verdadeira" que não filtra nada
            }
            return cb.equal(root.get("condominio").get("conCod"), condominioId);
        };
        return contratoRepository.findAll(spec).stream()
                .collect(Collectors.groupingBy(Contrato::getStatus, Collectors.counting()));
    }

    private void preencherDadosContrato(Contrato contrato, ContratoRequestDTO dto) {
        contrato.setEmpresa(dto.getEmpresa());
        contrato.setServico(dto.getServico());
        contrato.setValor(dto.getValor());
        contrato.setResponsavel(dto.getResponsavel());
        contrato.setDataInicio(dto.getDataInicio());
        contrato.setDataFim(dto.getDataFim());
        contrato.setObservacoes(dto.getObservacoes());
        
        if (dto.getId() != null && dto.getStatus() != null) {
            contrato.setStatus(dto.getStatus());
        } else {
            contrato.setStatus(calcularStatus(dto.getDataFim()));
        }
    }

    private StatusContrato calcularStatus(LocalDate dataFim) {
        LocalDate hoje = LocalDate.now();
        if (dataFim.isBefore(hoje)) return StatusContrato.FINALIZADO;
        if (dataFim.isBefore(hoje.plusDays(30))) return StatusContrato.A_VENCER;
        return StatusContrato.ATIVO;
    }
}