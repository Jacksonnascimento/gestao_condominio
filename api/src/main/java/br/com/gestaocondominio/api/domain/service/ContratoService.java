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
import org.springframework.util.StringUtils;

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

    public List<Contrato> listarContratos(Integer condominioId, String busca, StatusContrato status, Boolean isProximoVencimento, Boolean isHistorico, LocalDate inicioApos, LocalDate fimAntes) {
        Specification<Contrato> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (condominioId != null) {
                predicates.add(cb.equal(root.get("condominio").get("conCod"), condominioId));
            }

            if (StringUtils.hasText(busca)) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("empresa")), "%" + busca.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("servico")), "%" + busca.toLowerCase() + "%")
                ));
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

        if (Boolean.TRUE.equals(isHistorico)) {
            return todosContratos.stream()
                    .filter(c -> c.getStatus() == StatusContrato.FINALIZADO || c.getStatus() == StatusContrato.RESCINDIDO)
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

    public Map<StatusContrato, Long> contarContratosPorStatus(Integer condominioId) {
        Specification<Contrato> spec = (root, query, cb) -> {
            if (condominioId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("condominio").get("conCod"), condominioId);
        };

        List<Contrato> todosContratos = contratoRepository.findAll(spec);
        todosContratos.forEach(this::atualizarStatusCalculado);

        return todosContratos.stream()
                .collect(Collectors.groupingBy(Contrato::getStatus, Collectors.counting()));
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
        if (dataFim == null) return StatusContrato.ATIVO;
        if (dataFim.isBefore(hoje)) return StatusContrato.FINALIZADO;
        if (dataFim.isBefore(hoje.plusDays(30))) return StatusContrato.A_VENCER;
        return StatusContrato.ATIVO;
    }
}