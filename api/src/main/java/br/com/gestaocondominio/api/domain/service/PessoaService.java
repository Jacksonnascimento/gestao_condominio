package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.PessoaUpdateRequest;
import br.com.gestaocondominio.api.domain.entity.Administradora;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.repository.AdministradoraRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import br.com.gestaocondominio.api.util.ValidadorDocumento;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final CondominioRepository condominioRepository;
    private final AdministradoraRepository administradoraRepository;

    public PessoaService(PessoaRepository pessoaRepository,
                         PasswordEncoder passwordEncoder,
                         UsuarioCondominioRepository usuarioCondominioRepository,
                         CondominioRepository condominioRepository,
                         AdministradoraRepository administradoraRepository) {
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.condominioRepository = condominioRepository;
        this.administradoraRepository = administradoraRepository;
    }

    public Pessoa cadastrarPessoa(Pessoa pessoa) {
        if (!ValidadorDocumento.isValid(pessoa.getPesCpfCnpj())) {
            throw new IllegalArgumentException("O CPF/CNPJ informado é inválido.");
        }
        Optional<Pessoa> pessoaExistentePorCpfCnpj = pessoaRepository.findByPesCpfCnpj(pessoa.getPesCpfCnpj());
        if (pessoaExistentePorCpfCnpj.isPresent()) {
            throw new IllegalArgumentException("CPF/CNPJ já cadastrado no sistema.");
        }
        Optional<Pessoa> pessoaExistentePorEmail = pessoaRepository.findByPesEmail(pessoa.getPesEmail());
        if (pessoaExistentePorEmail.isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado no sistema.");
        }
        if (StringUtils.hasText(pessoa.getPesSenhaLogin())) {
            pessoa.setPesSenhaLogin(passwordEncoder.encode(pessoa.getPesSenhaLogin()));
        }
        pessoa.setPesDtCadastro(LocalDateTime.now());
        pessoa.setPesDtAtualizacao(LocalDateTime.now());
        if (pessoa.getPesAtivo() == null) {
            pessoa.setPesAtivo(true);
        }
        if (pessoa.getPesIsGlobalAdmin() == null) {
            pessoa.setPesIsGlobalAdmin(false);
        }
        return pessoaRepository.save(pessoa);
    }

    public List<Pessoa> listarPessoasAutorizadas() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return pessoaRepository.findAll();
        }

        Set<Pessoa> pessoasVisiveis = new HashSet<>();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Pessoa pessoaLogada = userDetails.getPessoa();
        
        pessoasVisiveis.add(pessoaLogada);

        Set<Integer> idsCondosGerenciados = getCondoIdsPorPapel(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_");

        Set<Integer> idsAdministradoras = getAdministradoraIds(authentication);
        if (!idsAdministradoras.isEmpty()) {
            List<Administradora> administradoras = administradoraRepository.findAllById(idsAdministradoras);
            if (!administradoras.isEmpty()) {
                List<Condominio> condominiosDaAdm = condominioRepository.findByAdministradoraIn(administradoras);
                condominiosDaAdm.forEach(condo -> idsCondosGerenciados.add(condo.getConCod()));
            }
        }

        if (!idsCondosGerenciados.isEmpty()) {
            List<Condominio> condominios = condominioRepository.findAllById(idsCondosGerenciados);
            List<UsuarioCondominio> associacoes = usuarioCondominioRepository.findByCondominioIn(condominios);
            associacoes.forEach(uc -> pessoasVisiveis.add(uc.getPessoa()));
        }
        
        return new ArrayList<>(pessoasVisiveis);
    }
    
    public Optional<Pessoa> buscarPessoaPorId(Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (userDetails.getPessoa().getPesCod().equals(id)) {
            return pessoaRepository.findById(id);
        }

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return pessoaRepository.findById(id);
        }
        
        Pessoa pessoaParaVisualizar = pessoaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + id));

        Set<Integer> idsCondosGerenciados = getCondoIdsPorPapel(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_");
        Set<Integer> idsAdministradoras = getAdministradoraIds(authentication);
        if (!idsAdministradoras.isEmpty()) {
            List<Administradora> administradoras = administradoraRepository.findAllById(idsAdministradoras);
            if (!administradoras.isEmpty()) {
                List<Condominio> condominiosDaAdm = condominioRepository.findByAdministradoraIn(administradoras);
                condominiosDaAdm.forEach(condo -> idsCondosGerenciados.add(condo.getConCod()));
            }
        }

        List<UsuarioCondominio> associacoesDaPessoaAlvo = usuarioCondominioRepository.findByPessoa(pessoaParaVisualizar);
        
        boolean temAcesso = associacoesDaPessoaAlvo.stream()
                .anyMatch(uc -> idsCondosGerenciados.contains(uc.getConCod()));
        
        if (temAcesso) {
            return Optional.of(pessoaParaVisualizar);
        }

        throw new AccessDeniedException("Acesso negado. Você não tem permissão para visualizar este perfil.");
    }

    public List<Pessoa> listarTodasPessoas() {
        return pessoaRepository.findAll();
    }
   
    public Pessoa atualizarPessoa(Integer id, PessoaUpdateRequest dadosParaAtualizar) {
        Pessoa pessoaNoBanco = pessoaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + id));

        if (dadosParaAtualizar.pesNome() != null) {
            pessoaNoBanco.setPesNome(dadosParaAtualizar.pesNome());
        }
        if (dadosParaAtualizar.pesTipo() != null) {
            pessoaNoBanco.setPesTipo(dadosParaAtualizar.pesTipo());
        }
        if (dadosParaAtualizar.pesTelefone() != null) {
            pessoaNoBanco.setPesTelefone(dadosParaAtualizar.pesTelefone());
        }
        if (dadosParaAtualizar.pesTelefone2() != null) {
            pessoaNoBanco.setPesTelefone2(dadosParaAtualizar.pesTelefone2());
        }

        if (dadosParaAtualizar.pesCpfCnpj() != null && !pessoaNoBanco.getPesCpfCnpj().equals(dadosParaAtualizar.pesCpfCnpj())) {
            if (!ValidadorDocumento.isValid(dadosParaAtualizar.pesCpfCnpj())) {
                throw new IllegalArgumentException("O novo CPF/CNPJ informado é inválido.");
            }
            pessoaRepository.findByPesCpfCnpj(dadosParaAtualizar.pesCpfCnpj()).ifPresent(p -> {
                if (!p.getPesCod().equals(id)) throw new IllegalArgumentException("Novo CPF/CNPJ já cadastrado para outra pessoa.");
            });
            pessoaNoBanco.setPesCpfCnpj(dadosParaAtualizar.pesCpfCnpj());
        }
        if (dadosParaAtualizar.pesEmail() != null && !pessoaNoBanco.getPesEmail().equals(dadosParaAtualizar.pesEmail())) {
            pessoaRepository.findByPesEmail(dadosParaAtualizar.pesEmail()).ifPresent(p -> {
                if (!p.getPesCod().equals(id)) throw new IllegalArgumentException("Novo E-mail já cadastrado para outra pessoa.");
            });
            pessoaNoBanco.setPesEmail(dadosParaAtualizar.pesEmail());
        }

        if (StringUtils.hasText(dadosParaAtualizar.pesSenhaLogin())) {
            pessoaNoBanco.setPesSenhaLogin(passwordEncoder.encode(dadosParaAtualizar.pesSenhaLogin()));
        }

        pessoaNoBanco.setPesDtAtualizacao(LocalDateTime.now());
        return pessoaRepository.save(pessoaNoBanco);
    }
    
    public Pessoa inativarPessoa(Integer id) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + id));
        pessoa.setPesAtivo(false);
        pessoa.setPesDtAtualizacao(LocalDateTime.now());
        return pessoaRepository.save(pessoa);
    }

    public Pessoa ativarPessoa(Integer id) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada com o ID: " + id));
        pessoa.setPesAtivo(true);
        pessoa.setPesDtAtualizacao(LocalDateTime.now());
        return pessoaRepository.save(pessoa);
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    private Set<Integer> getCondoIdsPorPapel(Authentication auth, String... prefixes) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authString -> Arrays.stream(prefixes).anyMatch(authString::startsWith))
                .map(authString -> Integer.parseInt(authString.substring(authString.lastIndexOf('_') + 1)))
                .collect(Collectors.toSet());
    }
    
    private Set<Integer> getAdministradoraIds(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authString -> authString.contains("_ADMINISTRADORA_"))
                .map(authString -> Integer.parseInt(authString.substring(authString.lastIndexOf('_') + 1)))
                .collect(Collectors.toSet());
    }
}