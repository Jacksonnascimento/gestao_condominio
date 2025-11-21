package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.VisitanteDTO;
import br.com.gestaocondominio.api.controller.dto.VisitanteRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.entity.Visitante;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.enums.VisitanteStatus;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.VisitanteRepository;
import br.com.gestaocondominio.api.domain.repository.VisitanteSpecification;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import br.com.gestaocondominio.api.util.ValidadorDocumento;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VisitanteService {

    private final VisitanteRepository visitanteRepository;
    private final UnidadeRepository unidadeRepository;
    private final CondominioRepository condominioRepository;
    private final PessoaRepository pessoaRepository;
    private final UsuarioCondominioService usuarioCondominioService;
    private final OcupanteRepository ocupanteRepository;

    public VisitanteService(VisitanteRepository visitanteRepository,
                            UnidadeRepository unidadeRepository,
                            CondominioRepository condominioRepository,
                            PessoaRepository pessoaRepository,
                            UsuarioCondominioService usuarioCondominioService,
                            OcupanteRepository ocupanteRepository) {
        this.visitanteRepository = visitanteRepository;
        this.unidadeRepository = unidadeRepository;
        this.condominioRepository = condominioRepository;
        this.pessoaRepository = pessoaRepository;
        this.usuarioCondominioService = usuarioCondominioService;
        this.ocupanteRepository = ocupanteRepository;
    }

    private boolean podeGerenciarVisitantes(Pessoa pessoa) {
        return pessoa.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(pessoa,
                UserRole.SINDICO, UserRole.ADMIN, UserRole.FUNCIONARIO_ADM, UserRole.PORTEIRO);
    }

    private Specification<Visitante> getSpec(Pessoa usuarioLogado, Integer condominioId, String nome, Integer unidadeId) {
        Integer idCondominioParaFiltrar = condominioId;
        Specification<Visitante> spec = Specification.where(null);

        if (usuarioLogado.getPesIsGlobalAdmin()) {
            // Admin Global vê tudo ou filtra
        } else if (podeGerenciarVisitantes(usuarioLogado)) {
            // Gestores veem tudo do seu condomínio
            if (idCondominioParaFiltrar == null) {
                idCondominioParaFiltrar = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
            }
        } else {
            // MORADOR: Vê apenas visitantes vinculados às suas unidades
            List<Unidade> unidadesDoMorador = ocupanteRepository.findByPessoa(usuarioLogado)
                    .stream()
                    .map(Ocupante::getUnidade)
                    .collect(Collectors.toList());

            if (unidadesDoMorador.isEmpty()) {
                // Se não tem unidade, não vê nada
                return (root, query, cb) -> cb.disjunction();
            }

            // Força o filtro para as unidades do morador
            spec = spec.and((root, query, cb) -> root.get("unidade").in(unidadesDoMorador));
            
            // Ignora filtro de condomínio explícito, pois a unidade já define o escopo
            idCondominioParaFiltrar = null; 
        }

        return spec.and(VisitanteSpecification.filtrar(idCondominioParaFiltrar, nome, unidadeId));
    }

    @Transactional(readOnly = true)
    public Page<VisitanteDTO> consultarVisitantes(Pessoa usuarioLogado, Integer condominioId, String nome, Integer unidadeId, Pageable pageable) {
        Specification<Visitante> spec = getSpec(usuarioLogado, condominioId, nome, unidadeId);
        Page<Visitante> page = visitanteRepository.findAll(spec, pageable);
        List<VisitanteDTO> dtos = page.getContent().stream()
                .map(VisitanteDTO::new)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> contarVisitantes(Pessoa usuarioLogado, Integer condominioId, String nome, Integer unidadeId) {
        Specification<Visitante> spec = getSpec(usuarioLogado, condominioId, nome, unidadeId);
        List<Visitante> visitantes = visitanteRepository.findAll(spec);

        LocalDateTime inicioDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime fimDoDia = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        long total = visitantes.size();
        long noLocal = visitantes.stream().filter(v -> v.getStatus() == VisitanteStatus.NO_LOCAL).count();
        
        long visitantesDoDia = visitantes.stream()
                .filter(v -> v.getDataEntrada().isAfter(inicioDoDia) && v.getDataEntrada().isBefore(fimDoDia))
                .count();

        long saidasDoDia = visitantes.stream()
                .filter(v -> v.getDataSaida() != null && v.getDataSaida().isAfter(inicioDoDia) && v.getDataSaida().isBefore(fimDoDia))
                .count();

        return Map.of(
                "TOTAL", total,
                "NO_LOCAL", noLocal,
                "DO_DIA", visitantesDoDia,
                "SAIDAS_DIA", saidasDoDia
        );
    }

    @Transactional
    public Visitante cadastrarVisitante(VisitanteRequestDTO dto, Pessoa usuarioLogado) {
        if (!podeGerenciarVisitantes(usuarioLogado)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }

        if (StringUtils.hasText(dto.getCpf())) {
            if (!ValidadorDocumento.isValid(dto.getCpf())) {
                throw new IllegalArgumentException("O CPF informado é inválido.");
            }
        }

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));

        Integer condoId = dto.getCondominioId();
        if (condoId == null && !usuarioLogado.getPesIsGlobalAdmin()) {
             condoId = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
        }
        
        if (condoId == null) {
             throw new IllegalArgumentException("Condomínio não identificado.");
        }

        Condominio condominio = condominioRepository.findById(condoId)
                .orElseThrow(() -> new EntityNotFoundException("Condomínio não encontrado."));

        if (!unidade.getCondominio().getConCod().equals(condominio.getConCod())) {
            throw new IllegalArgumentException("A unidade não pertence ao condomínio selecionado.");
        }

        Pessoa morador = null;
        if (dto.getMoradorId() != null) {
            morador = pessoaRepository.findById(dto.getMoradorId())
                    .orElseThrow(() -> new EntityNotFoundException("Morador não encontrado."));
        }

        Visitante visitante = new Visitante();
        visitante.setCondominio(condominio);
        visitante.setUnidade(unidade);
        visitante.setPessoaRegistro(usuarioLogado);
        visitante.setMoradorAutorizou(morador);
        visitante.setNome(dto.getNome());
        visitante.setCpf(dto.getCpf());
        visitante.setRg(dto.getRg());
        visitante.setTelefone(dto.getTelefone());
        visitante.setObservacoes(dto.getObservacoes());
        visitante.setStatus(VisitanteStatus.NO_LOCAL);
        visitante.setDataEntrada(LocalDateTime.now());
        visitante.setDataCadastro(LocalDateTime.now());
        visitante.setDataAtualizacao(LocalDateTime.now());

        return visitanteRepository.save(visitante);
    }

    @Transactional
    public Visitante atualizarVisitante(Integer id, VisitanteRequestDTO dto, Pessoa usuarioLogado) {
        Visitante visitante = buscarPorIdEValidarAcesso(id, usuarioLogado, true);

        if (StringUtils.hasText(dto.getCpf())) {
            if (!ValidadorDocumento.isValid(dto.getCpf())) {
                throw new IllegalArgumentException("O CPF informado é inválido.");
            }
        }

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada."));
        
        if (!unidade.getCondominio().getConCod().equals(visitante.getCondominio().getConCod())) {
             throw new IllegalArgumentException("A unidade deve pertencer ao mesmo condomínio.");
        }

        Pessoa morador = null;
        if (dto.getMoradorId() != null) {
            morador = pessoaRepository.findById(dto.getMoradorId())
                    .orElseThrow(() -> new EntityNotFoundException("Morador não encontrado."));
        }

        visitante.setNome(dto.getNome());
        visitante.setCpf(dto.getCpf());
        visitante.setRg(dto.getRg());
        visitante.setTelefone(dto.getTelefone());
        visitante.setUnidade(unidade);
        visitante.setMoradorAutorizou(morador);
        visitante.setObservacoes(dto.getObservacoes());
        visitante.setDataAtualizacao(LocalDateTime.now());

        return visitanteRepository.save(visitante);
    }

    @Transactional
    public Visitante registrarSaida(Integer id, Pessoa usuarioLogado) {
        Visitante visitante = buscarPorIdEValidarAcesso(id, usuarioLogado, true);

        if (visitante.getStatus() == VisitanteStatus.SAIU) {
            throw new IllegalArgumentException("Saída já registrada para este visitante.");
        }

        visitante.setStatus(VisitanteStatus.SAIU);
        visitante.setDataSaida(LocalDateTime.now());
        visitante.setDataAtualizacao(LocalDateTime.now());

        return visitanteRepository.save(visitante);
    }

    @Transactional(readOnly = true)
    public Visitante buscarPorIdEValidarAcesso(Integer id, Pessoa usuarioLogado, boolean paraEscrita) {
        Visitante visitante = visitanteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Visitante não encontrado."));

        if (usuarioLogado.getPesIsGlobalAdmin()) {
            return visitante;
        }

        if (podeGerenciarVisitantes(usuarioLogado)) {
            Integer conCodUsuario = usuarioCondominioService.getCondominioIdDoUsuario(usuarioLogado);
            if (visitante.getCondominio().getConCod().equals(conCodUsuario)) {
                return visitante;
            }
        }

        // Validação para Morador (apenas leitura)
        if (!paraEscrita) {
            boolean isMoradorDaUnidade = ocupanteRepository.findByPessoa(usuarioLogado)
                    .stream()
                    .anyMatch(o -> o.getUnidade().getUniCod().equals(visitante.getUnidade().getUniCod()));
            
            if (isMoradorDaUnidade) {
                return visitante;
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
    }
    
    @Transactional(readOnly = true)
    public VisitanteDTO buscarPorIdDTO(Integer id, Pessoa usuarioLogado) {
        Visitante visitante = buscarPorIdEValidarAcesso(id, usuarioLogado, false);
        return new VisitanteDTO(visitante);
    }
}