package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.EncomendaDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaRequestDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaRetiradaRequestDTO;
import br.com.gestaocondominio.api.controller.dto.EncomendaStatusRequestDTO;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.EncomendaStatus;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.EncomendaRepository;
import br.com.gestaocondominio.api.domain.repository.EncomendaSpecification;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EncomendaServiceImpl implements EncomendaService {

    @Autowired private EncomendaRepository encomendaRepository;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private CondominioRepository condominioRepository;
    @Autowired private OcupanteRepository ocupanteRepository;
    @Autowired private UsuarioCondominioService usuarioCondominioService;

    private boolean podeGerenciarEncomendas(Pessoa pessoa) {
        return pessoa.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(pessoa,
                UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO);
    }

    private Specification<Encomenda> getSpec(Pessoa usuarioLogado, Integer condominioId, String busca, Integer unidadeId, EncomendaStatus status) {
        Integer idCondominioParaFiltrar = condominioId;
        List<Unidade> unidadesPermitidas = null;

        if (usuarioLogado.getPesIsGlobalAdmin()) {
            // Admin Global: pode filtrar por condomínio se quiser, senão vê todos
        } else if (podeGerenciarEncomendas(usuarioLogado)) {
            // Gestor: Vê apenas o seu condomínio
            if (idCondominioParaFiltrar == null) {
                idCondominioParaFiltrar = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
            }
        } else {
            // Morador: Vê apenas suas unidades
            unidadesPermitidas = ocupanteRepository.findByPessoa(usuarioLogado)
                    .stream()
                    .map(Ocupante::getUnidade)
                    .toList();
            if (unidadesPermitidas.isEmpty()) {
                // Morador sem unidade não vê nada
                return (root, query, cb) -> cb.disjunction();
            }
            idCondominioParaFiltrar = null; // Filtro de condomínio é ignorado, prevalece a lista de unidades
        }

        return EncomendaSpecification.filtrar(idCondominioParaFiltrar, busca, unidadeId, status, unidadesPermitidas);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EncomendaDTO> consultarEncomendas(Pessoa usuarioLogado, Integer condominioId, String busca, Integer unidadeId, EncomendaStatus status, Pageable pageable) {
        Specification<Encomenda> spec = getSpec(usuarioLogado, condominioId, busca, unidadeId, status);
        Page<Encomenda> page = encomendaRepository.findAll(spec, pageable);
        List<EncomendaDTO> dtos = page.getContent().stream()
                .map(EncomendaDTO::new)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> contarStatusEncomendas(Pessoa usuarioLogado, Integer condominioId, String busca, Integer unidadeId, EncomendaStatus status) {
        Specification<Encomenda> specBase = getSpec(usuarioLogado, condominioId, busca, unidadeId, status);
        List<Encomenda> encomendas = encomendaRepository.findAll(specBase);

        Map<EncomendaStatus, Long> contagem = encomendas.stream()
                .collect(Collectors.groupingBy(Encomenda::getStatus, Collectors.counting()));

        return Map.of(
                "TOTAL", (long) encomendas.size(),
                "PENDENTES", contagem.getOrDefault(EncomendaStatus.PENDENTE, 0L),
                "RETIRADAS", contagem.getOrDefault(EncomendaStatus.RETIRADA, 0L),
                "DEVOLVIDAS", contagem.getOrDefault(EncomendaStatus.DEVOLVIDA, 0L),
                "EXTRAVIADAS", contagem.getOrDefault(EncomendaStatus.EXTRAVIADA, 0L)
        );
    }

    @Override
    @Transactional
    public Encomenda criarEncomenda(EncomendaRequestDTO dto, Pessoa usuarioLogado) {
        if (!podeGerenciarEncomendas(usuarioLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));

        Condominio condominio = condominioRepository.findById(dto.getCondominioId())
                .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado."));

        if (!unidade.getCondominio().getConCod().equals(condominio.getConCod())) {
             throw new IllegalArgumentException("A unidade não pertence ao condomínio selecionado.");
        }

        Encomenda encomenda = Encomenda.builder()
                .condominio(condominio)
                .unidade(unidade)
                .pessoaRegistro(usuarioLogado)
                .nomeRecebidoPor(dto.getNomeRecebidoPor())
                .destinatario(dto.getDestinatario())
                .descricao(dto.getDescricao())
                .tipo(dto.getTipo())
                .status(EncomendaStatus.PENDENTE)
                .dataRecebimento(LocalDateTime.of(dto.getDataRecebimento(), dto.getHoraRecebimento()))
                .observacoes(dto.getObservacoes())
                .build();
        
        return encomendaRepository.save(encomenda);
    }

    @Override
    @Transactional
    public Encomenda registrarRetirada(Long encomendaId, EncomendaRetiradaRequestDTO dto, Pessoa usuarioLogado) {
        Encomenda encomenda = buscarPorIdEValidarAcesso(encomendaId, usuarioLogado, true);

        if (encomenda.getStatus() != EncomendaStatus.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta encomenda não está pendente para retirada.");
        }

        encomenda.setStatus(EncomendaStatus.RETIRADA);
        encomenda.setDataRetirada(LocalDateTime.of(dto.getDataRetirada(), dto.getHoraRetirada()));
        encomenda.setNomeRetirada(dto.getNomeRetirada());
        encomenda.setPessoaRetirada(usuarioLogado);
        encomenda.setDataAtualizacaoStatus(LocalDateTime.now());

        return encomendaRepository.save(encomenda);
    }

    @Override
    @Transactional
    public Encomenda atualizarStatus(Long encomendaId, EncomendaStatusRequestDTO dto, Pessoa usuarioLogado) {
        Encomenda encomenda = buscarPorIdEValidarAcesso(encomendaId, usuarioLogado, true);

        if (dto.getNovoStatus() == EncomendaStatus.RETIRADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a funcionalidade 'Registrar Retirada' para este status.");
        }
        if (dto.getNovoStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum status selecionado.");
        }
        if (encomenda.getStatus() == EncomendaStatus.RETIRADA) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível alterar o status de uma encomenda já retirada.");
        }

        encomenda.setStatus(dto.getNovoStatus());
        encomenda.setObservacaoAtualizacao(dto.getObservacoes());
        encomenda.setDataAtualizacaoStatus(LocalDateTime.now());

        return encomendaRepository.save(encomenda);
    }

    @Override
    @Transactional(readOnly = true)
    public Encomenda buscarPorIdEValidarAcesso(Long id, Pessoa usuarioLogado, boolean paraEscrita) {
        Encomenda encomenda = encomendaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encomenda não encontrada com ID: " + id));

        if (usuarioLogado.getPesIsGlobalAdmin()) {
            return encomenda;
        }

        if (podeGerenciarEncomendas(usuarioLogado)) {
            Integer conCodEncomenda = encomenda.getCondominio().getConCod();
            boolean temAcessoCondo = usuarioCondominioService.findByPessoa(usuarioLogado).stream()
                    .anyMatch(uc -> uc.getConCod().equals(conCodEncomenda));
            if (temAcessoCondo) {
                return encomenda;
            }
        }

        if (paraEscrita) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado. Você não pode modificar esta encomenda.");
        }

        boolean isMoradorDaUnidade = ocupanteRepository.findByPessoa(usuarioLogado).stream()
                .anyMatch(oc -> oc.getUnidade().getUniCod().equals(encomenda.getUnidade().getUniCod()));

        if (isMoradorDaUnidade) {
            return encomenda;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
    }

    // NOVO MÉTODO ADICIONADO
    @Override
    @Transactional(readOnly = true)
    public EncomendaDTO buscarPorIdDTO(Long id, Pessoa usuarioLogado) {
        Encomenda encomenda = this.buscarPorIdEValidarAcesso(id, usuarioLogado, false); // false = apenas leitura
        return new EncomendaDTO(encomenda);
    }
}