package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.BalancoFinanceiroDTO;
import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca;
import br.com.gestaocondominio.api.domain.entity.FinanceiroDespesa;
import br.com.gestaocondominio.api.domain.enums.CobrancaStatus;
import br.com.gestaocondominio.api.domain.enums.DespesaStatusPagamento;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import br.com.gestaocondominio.api.domain.repository.FinanceiroCobrancaRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RelatorioFinanceiroService {

    private final FinanceiroCobrancaRepository financeiroCobrancaRepository;
    private final FinanceiroDespesaRepository financeiroDespesaRepository;
    private final CondominioRepository condominioRepository;

    public RelatorioFinanceiroService(FinanceiroCobrancaRepository financeiroCobrancaRepository,
                                      FinanceiroDespesaRepository financeiroDespesaRepository,
                                      CondominioRepository condominioRepository) {
        this.financeiroCobrancaRepository = financeiroCobrancaRepository;
        this.financeiroDespesaRepository = financeiroDespesaRepository;
        this.condominioRepository = condominioRepository;
    }

    @Transactional(readOnly = true)
    public BalancoFinanceiroDTO gerarBalançoFinanceiro(Integer condominioId, LocalDate dataInicio, LocalDate dataFim) {
        if (condominioId == null) {
            throw new IllegalArgumentException("ID do condomínio deve ser informado para gerar o balanço.");
        }
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Datas de início e fim devem ser informadas para gerar o balanço.");
        }
        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("Data de início não pode ser posterior à data de fim.");
        }

        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + condominioId));

        checkPermissionToViewBalance(condominio.getConCod());

        
        List<FinanceiroCobranca> receitas = financeiroCobrancaRepository
                .findByUnidade_CondominioAndFicStatusPagamentoAndFicDtPagamentoBetween(
                        condominio, CobrancaStatus.PAGA, dataInicio, dataFim);
        
        BigDecimal totalReceitas = receitas.stream()
                .map(FinanceiroCobranca::getFicValorPago) 
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        
        List<FinanceiroDespesa> despesas = financeiroDespesaRepository
                .findByCondominioAndDesStatusPagamentoAndDesDataPagamentoBetween(
                        condominio, DespesaStatusPagamento.PAGA, dataInicio, dataFim);
        
        BigDecimal totalDespesas = despesas.stream()
                .map(FinanceiroDespesa::getDesValor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal saldoFinal = totalReceitas.subtract(totalDespesas);

        return new BalancoFinanceiroDTO(condominioId, dataInicio, dataFim, totalReceitas, totalDespesas, saldoFinal);
    }

   
    private void checkPermissionToViewBalance(Integer condominioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) return;

        boolean hasAccess = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_", "ROLE_MORADOR_", "ROLE_FUNCIONARIO_ADM_", "ROLE_PORTEIRO_")
                            .contains(condominioId);
        
        if (!hasAccess) {
            throw new AccessDeniedException("Acesso negado para visualizar o balanço financeiro deste condomínio.");
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