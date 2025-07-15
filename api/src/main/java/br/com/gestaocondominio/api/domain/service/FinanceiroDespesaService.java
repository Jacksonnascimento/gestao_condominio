package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.DespesaCategoria;
import br.com.gestaocondominio.api.domain.entity.FinanceiroDespesa;
import br.com.gestaocondominio.api.domain.enums.DespesaStatusPagamento;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.DespesaCategoriaRepository;
import br.com.gestaocondominio.api.domain.repository.FinanceiroDespesaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FinanceiroDespesaService {

    private final FinanceiroDespesaRepository financeiroDespesaRepository;
    private final CondominioRepository condominioRepository;
    private final DespesaCategoriaRepository despesaCategoriaRepository;

    public FinanceiroDespesaService(FinanceiroDespesaRepository financeiroDespesaRepository,
                                    CondominioRepository condominioRepository,
                                    DespesaCategoriaRepository despesaCategoriaRepository) {
        this.financeiroDespesaRepository = financeiroDespesaRepository;
        this.condominioRepository = condominioRepository;
        this.despesaCategoriaRepository = despesaCategoriaRepository;
    }

    @Transactional
    public FinanceiroDespesa cadastrarDespesa(FinanceiroDespesa despesa) {
        if (despesa.getCondominio() == null || despesa.getCondominio().getConCod() == null) {
            throw new IllegalArgumentException("Condomínio deve ser informado para a despesa.");
        }
        Condominio condominio = condominioRepository.findById(despesa.getCondominio().getConCod())
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + despesa.getCondominio().getConCod()));
        despesa.setCondominio(condominio);

      
        hasPermissionToManageFinance(despesa.getCondominio().getConCod());

        if (despesa.getCategoria() == null || despesa.getCategoria().getDcaCod() == null) {
            throw new IllegalArgumentException("Categoria da despesa deve ser informada.");
        }
        DespesaCategoria categoria = despesaCategoriaRepository.findById(despesa.getCategoria().getDcaCod())
                .orElseThrow(() -> new IllegalArgumentException("Categoria de despesa não encontrada com o ID: " + despesa.getCategoria().getDcaCod()));
        despesa.setCategoria(categoria);

        if (despesa.getDesDescricao() == null || despesa.getDesDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição da despesa não pode ser vazia.");
        }

        if (despesa.getDesValor() == null || despesa.getDesValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da despesa deve ser maior que zero.");
        }

        if (despesa.getDesDataVencimento() == null) {
            throw new IllegalArgumentException("Data de vencimento da despesa deve ser informada.");
        }

        if (despesa.getDesStatusPagamento() == null) {
            despesa.setDesStatusPagamento(DespesaStatusPagamento.A_PAGAR);
        }

        return financeiroDespesaRepository.save(despesa);
    }

    @Transactional(readOnly = true)
    public Optional<FinanceiroDespesa> buscarDespesaPorId(Integer id) {
        Optional<FinanceiroDespesa> despesaOpt = financeiroDespesaRepository.findById(id);
        despesaOpt.ifPresent(this::checkPermissionToViewFinance);
        return despesaOpt;
    }

    @Transactional(readOnly = true)
    public List<FinanceiroDespesa> listarTodasDespesas(Integer condominioId, LocalDate dataInicio, LocalDate dataFim, Integer categoriaId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        List<Condominio> condominiosAcessiveis;

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            condominiosAcessiveis = condominioRepository.findAll();
        } else {
            Set<Integer> condoIds = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_", "ROLE_MORADOR_", "ROLE_FUNCIONARIO_ADM_", "ROLE_PORTEIRO_");
            if (condoIds.isEmpty()) {
                return List.of();
            }
            condominiosAcessiveis = condominioRepository.findAllById(condoIds);
        }

        if (condominioId != null) {
            condominiosAcessiveis = condominiosAcessiveis.stream()
                .filter(c -> c.getConCod().equals(condominioId))
                .collect(Collectors.toList());
            if (condominiosAcessiveis.isEmpty()) {
                 throw new AccessDeniedException("Acesso negado para listar despesas deste condomínio.");
            }
        }
        
      
        List<FinanceiroDespesa> despesasFiltradas = financeiroDespesaRepository.findByCondominioIn(condominiosAcessiveis);

        Stream<FinanceiroDespesa> filteredStream = despesasFiltradas.stream();

        if (dataInicio != null && dataFim != null) {
            filteredStream = filteredStream.filter(d -> 
                (d.getDesDataVencimento().isAfter(dataInicio.minusDays(1)) && d.getDesDataVencimento().isBefore(dataFim.plusDays(1)))
            );
        } else if (dataInicio != null) {
            filteredStream = filteredStream.filter(d -> d.getDesDataVencimento().isAfter(dataInicio.minusDays(1)));
        } else if (dataFim != null) {
            filteredStream = filteredStream.filter(d -> d.getDesDataVencimento().isBefore(dataFim.plusDays(1)));
        }


        if (categoriaId != null) {
            filteredStream = filteredStream.filter(d -> d.getCategoria().getDcaCod().equals(categoriaId));
        }

        return filteredStream.collect(Collectors.toList());
    }

    @Transactional
    public FinanceiroDespesa atualizarDespesa(Integer id, FinanceiroDespesa despesaAtualizada) {
        FinanceiroDespesa despesaExistente = financeiroDespesaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Despesa não encontrada com o ID: " + id));

        
        hasPermissionToManageFinance(despesaExistente.getCondominio().getConCod());

    
        if (despesaAtualizada.getCondominio() != null && !despesaAtualizada.getCondominio().getConCod().equals(despesaExistente.getCondominio().getConCod()) ||
            despesaAtualizada.getCategoria() != null && !despesaAtualizada.getCategoria().getDcaCod().equals(despesaExistente.getCategoria().getDcaCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Condomínio ou a Categoria de uma despesa existente.");
        }

        if (despesaAtualizada.getDesDescricao() != null) {
            despesaExistente.setDesDescricao(despesaAtualizada.getDesDescricao());
        }
        if (despesaAtualizada.getDesValor() != null) {
            if (despesaAtualizada.getDesValor().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Valor da despesa deve ser maior que zero.");
            }
            despesaExistente.setDesValor(despesaAtualizada.getDesValor());
        }
        if (despesaAtualizada.getDesDataVencimento() != null) {
            despesaExistente.setDesDataVencimento(despesaAtualizada.getDesDataVencimento());
        }
        if (despesaAtualizada.getDesDataPagamento() != null) {
            despesaExistente.setDesDataPagamento(despesaAtualizada.getDesDataPagamento());
        }
        if (despesaAtualizada.getDesStatusPagamento() != null) {
            despesaExistente.setDesStatusPagamento(despesaAtualizada.getDesStatusPagamento());
        }

        return financeiroDespesaRepository.save(despesaExistente);
    }

    @Transactional
    public void deletarDespesa(Integer id) {
        FinanceiroDespesa despesa = financeiroDespesaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Despesa não encontrada para exclusão com o ID: " + id));

       
        hasPermissionToManageFinance(despesa.getCondominio().getConCod());

        if (despesa.getDesStatusPagamento() == DespesaStatusPagamento.PAGA) {
            throw new IllegalArgumentException("Não é possível excluir uma despesa que já foi PAGA.");
        }

        financeiroDespesaRepository.delete(despesa);
    }


    public boolean hasPermissionToManageFinance(Integer condominioId) {
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

    private void checkPermissionToViewFinance(FinanceiroDespesa despesa) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) return;

        Integer condominioId = despesa.getCondominio().getConCod();
        boolean hasAccess = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_", "ROLE_MORADOR_", "ROLE_FUNCIONARIO_ADM_", "ROLE_PORTEIRO_")
                            .contains(condominioId);
        
        if (!hasAccess) {
            throw new AccessDeniedException("Acesso negado para visualizar esta despesa.");
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