package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Administradora;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.repository.AdministradoraRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository; 
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder; 
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdministradoraService {

    private final AdministradoraRepository administradoraRepository;
    private final PessoaRepository pessoaRepository;
    private final CondominioRepository condominioRepository;

   
    public AdministradoraService(AdministradoraRepository administradoraRepository,
                                 PessoaRepository pessoaRepository,
                                 CondominioRepository condominioRepository) {
        this.administradoraRepository = administradoraRepository;
        this.pessoaRepository = pessoaRepository;
        this.condominioRepository = condominioRepository;
    }

    public Administradora cadastrarAdministradora(Administradora administradora) {
        if (administradora.getDadosEmpresa() == null || administradora.getDadosEmpresa().getPesCod() == null) {
            throw new IllegalArgumentException("Dados da Empresa (Pessoa) devem ser informados para a administradora.");
        }
        pessoaRepository.findById(administradora.getDadosEmpresa().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa (Dados da Empresa) não encontrada com o ID: " + administradora.getDadosEmpresa().getPesCod()));

        if (administradora.getResponsavel() == null || administradora.getResponsavel().getPesCod() == null) {
            throw new IllegalArgumentException("Pessoa Responsável deve ser informada para a administradora.");
        }
        pessoaRepository.findById(administradora.getResponsavel().getPesCod())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa (Responsável) não encontrada com o ID: " + administradora.getResponsavel().getPesCod()));

        administradora.setAdmDtCadastro(LocalDateTime.now());
        administradora.setAdmDtAtualizacao(LocalDateTime.now());
        if (administradora.getAdmAtivo() == null) {
            administradora.setAdmAtivo(true);
        }
        return administradoraRepository.save(administradora);
    }

    
    public Optional<Administradora> buscarAdministradoraPorId(Integer id) {
        Optional<Administradora> administradoraOpt = administradoraRepository.findById(id);
        administradoraOpt.ifPresent(this::checkPermissionToView); // Adiciona a verificação
        return administradoraOpt;
    }

    public List<Administradora> listarTodasAdministradoras(boolean incluirInativas) {
        
        if (incluirInativas) {
            return administradoraRepository.findAll();
        }
        return administradoraRepository.findByAdmAtivo(true);
    }

    public Administradora atualizarAdministradora(Integer id, Administradora administradoraAtualizada) {
        Administradora administradoraExistente = administradoraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Administradora não encontrada com o ID: " + id));

        if (administradoraAtualizada.getDadosEmpresa() != null && !administradoraAtualizada.getDadosEmpresa().getPesCod().equals(administradoraExistente.getDadosEmpresa().getPesCod())) {
            pessoaRepository.findById(administradoraAtualizada.getDadosEmpresa().getPesCod())
                    .orElseThrow(() -> new IllegalArgumentException("Nova Pessoa (Dados da Empresa) não encontrada com o ID: " + administradoraAtualizada.getDadosEmpresa().getPesCod()));
            administradoraExistente.setDadosEmpresa(administradoraAtualizada.getDadosEmpresa());
        } else if (administradoraAtualizada.getDadosEmpresa() == null) {
            throw new IllegalArgumentException("Dados da Empresa (Pessoa) não podem ser nulos na atualização.");
        }
        
        if (administradoraAtualizada.getResponsavel() != null && !administradoraAtualizada.getResponsavel().getPesCod().equals(administradoraExistente.getResponsavel().getPesCod())) {
            pessoaRepository.findById(administradoraAtualizada.getResponsavel().getPesCod())
                    .orElseThrow(() -> new IllegalArgumentException("Nova Pessoa (Responsável) não encontrada com o ID: " + administradoraAtualizada.getResponsavel().getPesCod()));
            administradoraExistente.setResponsavel(administradoraAtualizada.getResponsavel());
        } else if (administradoraAtualizada.getResponsavel() == null) {
            throw new IllegalArgumentException("Pessoa Responsável não pode ser nula na atualização.");
        }

        if (administradoraAtualizada.getAdmAtivo() != null) {
            administradoraExistente.setAdmAtivo(administradoraAtualizada.getAdmAtivo());
        }

        administradoraExistente.setAdmDtAtualizacao(LocalDateTime.now());
        return administradoraRepository.save(administradoraExistente);
    }

    public Administradora inativarAdministradora(Integer id) {
        Administradora administradora = administradoraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Administradora não encontrada com o ID: " + id));
        administradora.setAdmAtivo(false);
        administradora.setAdmDtAtualizacao(LocalDateTime.now());
        return administradoraRepository.save(administradora);
    }

    public Administradora ativarAdministradora(Integer id) {
        Administradora administradora = administradoraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Administradora não encontrada com o ID: " + id));
        administradora.setAdmAtivo(true);
        administradora.setAdmDtAtualizacao(LocalDateTime.now());
        return administradoraRepository.save(administradora);
    }

    
    private void checkPermissionToView(Administradora administradora) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GLOBAL_ADMIN"))) {
            return;
        }

       
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GERENTE_ADMINISTRADORA_" + administradora.getAdmCod()))) {
            return;
        }

       
        List<Condominio> condominiosGerenciados = condominioRepository.findByAdministradora(administradora);
        boolean isSindicoDeCondominioGerenciado = condominiosGerenciados.stream()
                .anyMatch(condo -> authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_SINDICO_" + condo.getConCod())));

        if (isSindicoDeCondominioGerenciado) {
            return;
        }

       
        throw new AccessDeniedException("Acesso negado. Você não tem permissão para visualizar esta administradora.");
    }
}