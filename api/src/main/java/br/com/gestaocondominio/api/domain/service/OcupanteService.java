package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.OcupanteRequestDTO;
import br.com.gestaocondominio.api.domain.entity.Ocupante;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.OcupanteVinculo;
import br.com.gestaocondominio.api.domain.repository.OcupanteRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service("ocupanteService")
public class OcupanteService {

    private final OcupanteRepository ocupanteRepository;
    private final PessoaRepository pessoaRepository;
    private final UnidadeRepository unidadeRepository;
    private final PessoaService pessoaService;

    public OcupanteService(OcupanteRepository ocupanteRepository, PessoaRepository pessoaRepository, UnidadeRepository unidadeRepository, PessoaService pessoaService) {
        this.ocupanteRepository = ocupanteRepository;
        this.pessoaRepository = pessoaRepository;
        this.unidadeRepository = unidadeRepository;
        this.pessoaService = pessoaService;
    }

    @Transactional(readOnly = true)
    public List<Ocupante> consultarOcupantes(String busca, String vinculo, Integer unidadeId) {
        List<Ocupante> todosOcupantes = ocupanteRepository.findAllWithDetails();
        Stream<Ocupante> stream = todosOcupantes.stream();

        if (busca != null && !busca.isBlank()) {
            String buscaLower = busca.toLowerCase().trim();
            stream = stream.filter(ocupante ->
                (ocupante.getPessoa().getPesNome() != null && ocupante.getPessoa().getPesNome().toLowerCase().contains(buscaLower)) ||
                (ocupante.getPessoa().getPesEmail() != null && ocupante.getPessoa().getPesEmail().toLowerCase().contains(buscaLower))
            );
        }

        if (vinculo != null && !vinculo.isBlank()) {
            try {
                OcupanteVinculo tipo = OcupanteVinculo.valueOf(vinculo.toUpperCase());
                stream = stream.filter(ocupante -> ocupante.getOcuVinculo() == tipo);
            } catch (IllegalArgumentException e) { /* Ignora filtro inválido */ }
        }

        if (unidadeId != null) {
            stream = stream.filter(ocupante -> ocupante.getUnidade().getUniCod().equals(unidadeId));
        }

        return stream.toList();
    }

    @Transactional(readOnly = true)
    public Optional<Ocupante> buscarPorId(Integer id) {
        return ocupanteRepository.findById(id);
    }

    @Transactional
    public Ocupante cadastrarOcupante(OcupanteRequestDTO dto) {
        if (dto.unidadeId() == null || dto.vinculo() == null || dto.inicioOcupacao() == null || dto.pesCpfCnpj() == null || dto.pesCpfCnpj().isBlank()) {
            throw new IllegalArgumentException("CPF/CNPJ, Unidade, Vínculo e Início da Ocupação são obrigatórios.");
        }

        Pessoa pessoa = pessoaRepository.findByPesCpfCnpj(dto.pesCpfCnpj())
            .orElseGet(() -> {
                Pessoa novaPessoa = new Pessoa();
                novaPessoa.setPesNome(dto.pesNome());
                novaPessoa.setPesCpfCnpj(dto.pesCpfCnpj());
                novaPessoa.setPesTipo(dto.pesTipo());
                novaPessoa.setPesEmail(dto.pesEmail());
                novaPessoa.setPesTelefone(dto.pesTelefone());
                return pessoaService.cadastrarPessoa(novaPessoa);
            });

        Unidade unidade = unidadeRepository.findById(dto.unidadeId())
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + dto.unidadeId()));

        ocupanteRepository.findByPessoaAndUnidade(pessoa, unidade).ifPresent(m -> {
            throw new IllegalArgumentException("Esta pessoa já está cadastrada como ocupante desta unidade.");
        });

        Ocupante novoOcupante = new Ocupante();
        novoOcupante.setPessoa(pessoa);
        novoOcupante.setUnidade(unidade);
        novoOcupante.setOcuVinculo(dto.vinculo());
        novoOcupante.setOcuDtInicioOcupacao(dto.inicioOcupacao());
        novoOcupante.setOcuDtFimOcupacao(dto.fimOcupacao());

        if (dto.vinculo() == OcupanteVinculo.MULTIPROPRIETARIO) {
            novoOcupante.setOcuPeriodoUso(dto.periodoUso());
            novoOcupante.setOcuTipoPeriodo(dto.tipoPeriodo());
        }

        novoOcupante.setOcuDtCadastro(LocalDateTime.now());
        novoOcupante.setOcuDtAtualizacao(LocalDateTime.now());

        return ocupanteRepository.save(novoOcupante);
    }

    @Transactional
    public Ocupante editarOcupante(Integer id, OcupanteRequestDTO dto) {
        Ocupante ocupanteExistente = ocupanteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ocupante não encontrado com o ID: " + id));

        Pessoa pessoaParaAtualizar = ocupanteExistente.getPessoa();
        pessoaParaAtualizar.setPesNome(dto.pesNome());
        pessoaParaAtualizar.setPesEmail(dto.pesEmail());
        pessoaParaAtualizar.setPesTelefone(dto.pesTelefone());
        pessoaRepository.save(pessoaParaAtualizar);

        ocupanteExistente.setOcuVinculo(dto.vinculo());
        ocupanteExistente.setOcuDtInicioOcupacao(dto.inicioOcupacao());
        ocupanteExistente.setOcuDtFimOcupacao(dto.fimOcupacao());

        if (dto.vinculo() == OcupanteVinculo.MULTIPROPRIETARIO) {
            ocupanteExistente.setOcuPeriodoUso(dto.periodoUso());
            ocupanteExistente.setOcuTipoPeriodo(dto.tipoPeriodo());
        } else {
            ocupanteExistente.setOcuPeriodoUso(null);
            ocupanteExistente.setOcuTipoPeriodo(null);
        }

        ocupanteExistente.setOcuDtAtualizacao(LocalDateTime.now());

        return ocupanteRepository.save(ocupanteExistente);
    }

    @Transactional
    public void excluirOcupante(Integer id) {
        if (!ocupanteRepository.existsById(id)) {
            throw new IllegalArgumentException("Ocupante não encontrado com o ID: " + id);
        }
        ocupanteRepository.deleteById(id);
    }

    public boolean temPermissaoParaGerenciar(Integer ocupanteId) {
        Ocupante ocupante = ocupanteRepository.findById(ocupanteId)
            .orElseThrow(() -> new IllegalArgumentException("Ocupante não encontrado com o ID: " + ocupanteId));

        Integer condominioId = ocupante.getUnidade().getCondominio().getConCod();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication.getAuthorities().stream()
                .anyMatch(auth -> {
                    String authority = auth.getAuthority();
                    return authority.equals("ROLE_GLOBAL_ADMIN") ||
                           authority.equals("ROLE_SINDICO_" + condominioId) ||
                           authority.equals("ROLE_ADMIN_" + condominioId);
                });
    }
}