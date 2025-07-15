// src/main/java/br/com/gestaocondominio/api/domain/service/DespesaCategoriaService.java
package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.DespesaCategoria;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.DespesaCategoriaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DespesaCategoriaService {

    private final DespesaCategoriaRepository despesaCategoriaRepository;
    private final CondominioRepository condominioRepository;

    public DespesaCategoriaService(DespesaCategoriaRepository despesaCategoriaRepository,
                                   CondominioRepository condominioRepository) {
        this.despesaCategoriaRepository = despesaCategoriaRepository;
        this.condominioRepository = condominioRepository;
    }

    @Transactional
    public DespesaCategoria cadastrarDespesaCategoria(DespesaCategoria categoria) {
        if (categoria.getCondominio() == null || categoria.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a categoria de despesa.");
        }
        Condominio condominio = condominioRepository.findById(categoria.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + categoria.getCondominio().getConCod()));
        
        
        hasPermissionToManageCategory(condominio.getConCod());

        despesaCategoriaRepository.findByDcaDescricaoAndCondominio(categoria.getDcaDescricao(), condominio)
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Já existe uma categoria de despesa com esta descrição para este condomínio: " + categoria.getDcaDescricao());
                });

        categoria.setCondominio(condominio);
        if (categoria.getDcaAtiva() == null) {
            categoria.setDcaAtiva(true);
        }
        return despesaCategoriaRepository.save(categoria);
    }

    @Transactional(readOnly = true)
    public Optional<DespesaCategoria> buscarDespesaCategoriaPorId(Integer id) {
        Optional<DespesaCategoria> categoriaOpt = despesaCategoriaRepository.findById(id);
        categoriaOpt.ifPresent(this::checkPermissionToViewCategory);
        return categoriaOpt;
    }

    @Transactional(readOnly = true)
    public List<DespesaCategoria> listarTodasDespesaCategorias(boolean incluirInativas) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return incluirInativas ? despesaCategoriaRepository.findAll() : despesaCategoriaRepository.findByDcaAtiva(true);
        }

        Set<Integer> condoIds = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_", "ROLE_MORADOR_", "ROLE_FUNCIONARIO_ADM_", "ROLE_PORTEIRO_");
        if (condoIds.isEmpty()) {
            return List.of();
        }

        List<Condominio> condominiosPermitidos = condominioRepository.findAllById(condoIds);
        
        Stream<DespesaCategoria> categoriaStream = condominiosPermitidos.stream()
            .flatMap(condo -> {
                if (incluirInativas) {
                    return despesaCategoriaRepository.findByCondominioIn(List.of(condo)).stream();
                } else {
                    return despesaCategoriaRepository.findByCondominioAndDcaAtiva(condo, true).stream();
                }
            });

        return categoriaStream.distinct().collect(Collectors.toList());
    }

    @Transactional
    public DespesaCategoria atualizarDespesaCategoria(Integer id, DespesaCategoria categoriaAtualizada) {
        DespesaCategoria categoriaExistente = despesaCategoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria de despesa não encontrada com o ID: " + id));
        
        
        hasPermissionToManageCategory(categoriaExistente.getCondominio().getConCod());

        if (categoriaAtualizada.getDcaDescricao() == null || categoriaAtualizada.getDcaDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição da categoria de despesa não pode ser vazia na atualização.");
        }

        if (!categoriaAtualizada.getDcaDescricao().equalsIgnoreCase(categoriaExistente.getDcaDescricao())) {
            despesaCategoriaRepository.findByDcaDescricaoAndCondominio(categoriaAtualizada.getDcaDescricao(), categoriaExistente.getCondominio())
                    .ifPresent(c -> {
                        if (!c.getDcaCod().equals(id)) {
                            throw new IllegalArgumentException("Nova descrição já cadastrada para outra categoria de despesa neste condomínio: " + categoriaAtualizada.getDcaDescricao());
                        }
                    });
        }
        
        categoriaExistente.setDcaDescricao(categoriaAtualizada.getDcaDescricao());
        if (categoriaAtualizada.getDcaAtiva() != null) {
            categoriaExistente.setDcaAtiva(categoriaAtualizada.getDcaAtiva());
        }

        return despesaCategoriaRepository.save(categoriaExistente);
    }

    @Transactional
    public DespesaCategoria inativarDespesaCategoria(Integer id) {
        DespesaCategoria categoria = despesaCategoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria de despesa não encontrada com o ID: " + id));
        
        
        hasPermissionToManageCategory(categoria.getCondominio().getConCod());

        categoria.setDcaAtiva(false);
        return despesaCategoriaRepository.save(categoria);
    }

    @Transactional
    public DespesaCategoria ativarDespesaCategoria(Integer id) {
        DespesaCategoria categoria = despesaCategoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria de despesa não encontrada com o ID: " + id));
        
        
        hasPermissionToManageCategory(categoria.getCondominio().getConCod());
        
        categoria.setDcaAtiva(true);
        return despesaCategoriaRepository.save(categoria);
    }

    
    public boolean hasPermissionToManageCategory(Integer condominioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) return true;
        if (hasAuthority(authentication, "ROLE_SINDICO_" + condominioId)) return true;
        if (hasAuthority(authentication, "ROLE_ADMIN_" + condominioId)) return true;

        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado."));

        if (condominio.getAdministradora() != null && 
            hasAuthority(authentication, "ROLE_GERENTE_ADMINISTRADORA_" + condominio.getAdministradora().getAdmCod())) {
            return true;
        }

        return false;
    }

    private void checkPermissionToViewCategory(DespesaCategoria categoria) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) return;

        Integer condominioId = categoria.getCondominio().getConCod();
        boolean hasAccess = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_", "ROLE_MORADOR_", "ROLE_FUNCIONARIO_ADM_", "ROLE_PORTEIRO_")
                            .contains(condominioId);
        
        if (!hasAccess) {
            throw new AccessDeniedException("Acesso negado para visualizar esta categoria de despesa.");
        }
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    private Set<Integer> getCondoIdsFromRoles(Authentication auth, String... prefixes) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authString -> Arrays.stream(prefixes).anyMatch(authString::startsWith))
                .map(authString -> {
                    try {
                        return Integer.parseInt(authString.substring(authString.lastIndexOf('_') + 1));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}