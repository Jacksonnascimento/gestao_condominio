package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.OcupanteRequestDTO;
import br.com.gestaocondominio.api.controller.dto.OcupanteResponseDTO;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.enums.UserRole;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.OcupanteSpecification;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("ocupanteService")
public class OcupanteService {

    @Autowired
    private OcupanteRepository ocupanteRepository;
    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private UnidadeRepository unidadeRepository;
    @Autowired
    private PessoaService pessoaService;
    @Autowired
    private UsuarioCondominioService usuarioCondominioService;

    @Transactional(readOnly = true)
    public List<OcupanteResponseDTO> consultarOcupantesPorUsuario(Pessoa usuario, Integer condominioId, String busca,
            OcupanteVinculo vinculo, Integer unidadeId) {
        List<Ocupante> ocupantes = findOcupantesByUsuario(usuario, condominioId, busca, vinculo, unidadeId);
        return ocupantes.stream()
                .map(OcupanteResponseDTO::new)
                .collect(Collectors.toList());
    }

    private List<Ocupante> findOcupantesByUsuario(Pessoa usuario, Integer condominioId, String busca,
            OcupanteVinculo vinculo, Integer unidadeId) {
        if (usuario.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuario, UserRole.SINDICO,
                UserRole.ADMIN, UserRole.FUNCIONARIO_ADM)) {
            Specification<Ocupante> spec = OcupanteSpecification.comFiltros(condominioId, busca, vinculo, unidadeId);
            return ocupanteRepository.findAll(spec);
        } else {
            Optional<Ocupante> ocupanteOpt = ocupanteRepository.findByPessoa(usuario).stream().findFirst();
            if (ocupanteOpt.isPresent()) {
                Unidade unidadeDoMorador = ocupanteOpt.get().getUnidade();
                return ocupanteRepository.findByUnidade(unidadeDoMorador);
            }
        }
        return Collections.emptyList();
    }

    // Assinatura sobrecarregada para não quebrar o controller antigo
    public List<OcupanteResponseDTO> consultarOcupantesPorUsuario(Pessoa usuario, Integer condominioId, String busca,
            OcupanteVinculo vinculo) {
        return this.consultarOcupantesPorUsuario(usuario, condominioId, busca, vinculo, null);
    }

    @Transactional(readOnly = true)
    public Map<OcupanteVinculo, Long> contarOcupantesPorUsuario(Pessoa usuario, Integer condominioId) {
        List<Ocupante> ocupantes = findOcupantesByUsuario(usuario, condominioId, null, null, null);
        return ocupantes.stream()
                .collect(Collectors.groupingBy(Ocupante::getOcuVinculo, Collectors.counting()));
    }

    // O restante da classe permanece igual...
    @Transactional
    public Ocupante cadastrarOcupante(OcupanteRequestDTO dto) {
        if (dto.getUnidadeId() == null || dto.getVinculo() == null || dto.getInicioOcupacao() == null
                || dto.getPesCpfCnpj() == null || dto.getPesCpfCnpj().isBlank()) {
            throw new IllegalArgumentException("CPF/CNPJ, Unidade, Vínculo e Início da Ocupação são obrigatórios.");
        }

        Pessoa pessoa = pessoaRepository.findByPesCpfCnpj(dto.getPesCpfCnpj())
                .orElseGet(() -> {
                    Pessoa novaPessoa = new Pessoa();
                    novaPessoa.setPesNome(dto.getPesNome());
                    novaPessoa.setPesCpfCnpj(dto.getPesCpfCnpj());
                    novaPessoa.setPesTipo(dto.getPesTipo());
                    novaPessoa.setPesEmail(dto.getPesEmail());
                    novaPessoa.setPesTelefone(dto.getPesTelefone());
                    return pessoaService.cadastrarPessoa(novaPessoa);
                });

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Unidade não encontrada com o ID: " + dto.getUnidadeId()));

        ocupanteRepository.findByPessoaAndUnidade(pessoa, unidade).ifPresent(m -> {
            throw new IllegalArgumentException("Esta pessoa já está cadastrada como ocupante desta unidade.");
        });

        Ocupante novoOcupante = new Ocupante();
        novoOcupante.setPessoa(pessoa);
        novoOcupante.setUnidade(unidade);
        novoOcupante.setOcuVinculo(dto.getVinculo());
        novoOcupante.setOcuDtInicioOcupacao(dto.getInicioOcupacao());
        novoOcupante.setOcuDtFimOcupacao(dto.getFimOcupacao());

        if (dto.getVinculo() == OcupanteVinculo.MULTIPROPRIETARIO) {
            novoOcupante.setOcuPeriodoUso(dto.getPeriodoUso());
            novoOcupante.setOcuTipoPeriodo(dto.getTipoPeriodo());
        }

        novoOcupante.setOcuDtCadastro(LocalDateTime.now());
        novoOcupante.setOcuDtAtualizacao(LocalDateTime.now());

        return ocupanteRepository.save(novoOcupante);
    }

    @Transactional
    public Ocupante editarOcupante(Integer id, OcupanteRequestDTO dto, Pessoa usuarioLogado) {
        Ocupante ocupanteExistente = buscarPorIdEValidarAcesso(id, usuarioLogado);

        Pessoa pessoaParaAtualizar = ocupanteExistente.getPessoa();
        pessoaParaAtualizar.setPesNome(dto.getPesNome());
        pessoaParaAtualizar.setPesEmail(dto.getPesEmail());
        pessoaParaAtualizar.setPesTelefone(dto.getPesTelefone());
        pessoaRepository.save(pessoaParaAtualizar);

        ocupanteExistente.setOcuVinculo(dto.getVinculo());
        ocupanteExistente.setOcuDtInicioOcupacao(dto.getInicioOcupacao());
        ocupanteExistente.setOcuDtFimOcupacao(dto.getFimOcupacao());

        if (dto.getVinculo() == OcupanteVinculo.MULTIPROPRIETARIO) {
            ocupanteExistente.setOcuPeriodoUso(dto.getPeriodoUso());
            ocupanteExistente.setOcuTipoPeriodo(dto.getTipoPeriodo());
        } else {
            ocupanteExistente.setOcuPeriodoUso(null);
            ocupanteExistente.setOcuTipoPeriodo(null);
        }

        ocupanteExistente.setOcuDtAtualizacao(LocalDateTime.now());
        return ocupanteRepository.save(ocupanteExistente);
    }

    @Transactional
    public void excluirOcupante(Integer id, Pessoa usuarioLogado) {
        Ocupante ocupante = buscarPorIdEValidarAcesso(id, usuarioLogado);
        ocupanteRepository.delete(ocupante);
    }

    @Transactional(readOnly = true)
    public Ocupante buscarPorIdEValidarAcesso(Integer id, Pessoa usuario) {
        Ocupante ocupante = ocupanteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocupante não encontrado"));

        if (usuario.getPesIsGlobalAdmin() || usuarioCondominioService.possuiRole(usuario, UserRole.SINDICO,
                UserRole.ADMIN, UserRole.FUNCIONARIO_ADM)) {
            return ocupante;
        }

        if (usuarioCondominioService.possuiRole(usuario, UserRole.MORADOR)) {
            Optional<Ocupante> moradorOcupanteOpt = ocupanteRepository.findByPessoa(usuario).stream().findFirst();
            if (moradorOcupanteOpt.isPresent() && moradorOcupanteOpt.get().getUnidade().equals(ocupante.getUnidade())) {
                return ocupante;
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso Negado");
    }

    @Transactional(readOnly = true)
    public Optional<Unidade> findUnidadeByMorador(Pessoa morador) {
        return ocupanteRepository.findByPessoa(morador)
                .stream()
                .findFirst()
                .map(Ocupante::getUnidade);
    }
}