package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.controller.dto.FinanceiroCobrancaRequestDTO;
import br.com.gestaocondominio.api.controller.dto.GerarCobrancaIndividualRequestDTO;
import br.com.gestaocondominio.api.controller.dto.GerarCobrancaLoteRequestDTO;
import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.CobrancaStatus;
import br.com.gestaocondominio.api.domain.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FinanceiroCobrancaService {

    private final FinanceiroCobrancaRepository financeiroCobrancaRepository;
    private final UnidadeRepository unidadeRepository;
    private final TipoCobrancaRepository tipoCobrancaRepository;
    private final CondominioRepository condominioRepository;
    private final TaxaCondominioValorRepository taxaCondominioValorRepository;

    public FinanceiroCobrancaService(FinanceiroCobrancaRepository financeiroCobrancaRepository,
                                     UnidadeRepository unidadeRepository,
                                     TipoCobrancaRepository tipoCobrancaRepository,
                                     CondominioRepository condominioRepository,
                                     TaxaCondominioValorRepository taxaCondominioValorRepository) {
        this.financeiroCobrancaRepository = financeiroCobrancaRepository;
        this.unidadeRepository = unidadeRepository;
        this.tipoCobrancaRepository = tipoCobrancaRepository;
        this.condominioRepository = condominioRepository;
        this.taxaCondominioValorRepository = taxaCondominioValorRepository;
    }

    @Transactional
    public FinanceiroCobranca gerarCobrancaIndividual(GerarCobrancaIndividualRequestDTO request) {
        Unidade unidade = unidadeRepository.findById(request.getUnidadeId())
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + request.getUnidadeId()));

        internalCheckPermissionToManageCobranca(unidade.getCondominio().getConCod());
        
        TipoCobranca tipoCobranca = tipoCobrancaRepository.findById(request.getTipoCobrancaId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de Cobrança não encontrado com o ID: " + request.getTipoCobrancaId()));

        verificarCobrancaDuplicada(unidade.getUniCod(), tipoCobranca.getTicCod(), request.getDataVencimento());

        BigDecimal valorFinal = calcularValorCobranca(unidade, tipoCobranca, Optional.ofNullable(request.getValorOpcional()));
        
        return criarRegistroDeCobranca(unidade, tipoCobranca, valorFinal, request.getDataVencimento());
    }

    @Transactional
    public void gerarCobrancasEmLote(GerarCobrancaLoteRequestDTO request) {
        internalCheckPermissionToManageCobranca(request.getCondominioId());

        List<Unidade> unidadesAtivas = unidadeRepository.findByCondominioConCodAndUniAtivaTrue(request.getCondominioId());
        TipoCobranca tipoCobranca = tipoCobrancaRepository.findById(request.getTipoCobrancaId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de Cobrança não encontrado com o ID: " + request.getTipoCobrancaId()));

        for (Unidade unidade : unidadesAtivas) {
            try {
                verificarCobrancaDuplicada(unidade.getUniCod(), tipoCobranca.getTicCod(), request.getDataVencimento());
                BigDecimal valorFinal = calcularValorCobranca(unidade, tipoCobranca, Optional.empty());
                criarRegistroDeCobranca(unidade, tipoCobranca, valorFinal, request.getDataVencimento());
            } catch (IllegalStateException e) {
                System.err.println("INFO: Cobrança ignorada para a unidade " + unidade.getUniCod() + ". Motivo: " + e.getMessage());
            }
        }
    }

    private void verificarCobrancaDuplicada(Integer unidadeId, Integer tipoCobrancaId, LocalDate dataCompetencia) {
        LocalDate inicioMes = dataCompetencia.withDayOfMonth(1);
        LocalDate fimMes = dataCompetencia.withDayOfMonth(dataCompetencia.lengthOfMonth());
        
        Optional<FinanceiroCobranca> cobrancaExistente = financeiroCobrancaRepository.findByCompetencia(
            unidadeId, tipoCobrancaId, inicioMes, fimMes
        );

        if (cobrancaExistente.isPresent() && cobrancaExistente.get().getFicStatusPagamento() != CobrancaStatus.CANCELADA) {
            throw new IllegalStateException("Cobrança duplicada (não cancelada) já existe para esta unidade e competência.");
        }
    }

    private BigDecimal calcularValorCobranca(Unidade unidade, TipoCobranca tipoCobranca, Optional<BigDecimal> valorOpcional) {
        if (Boolean.TRUE.equals(tipoCobranca.getTicIsTaxaPrincipal())) {
            if (unidade.getUnidadeTipo() == null) {
                throw new IllegalStateException("A unidade não possui um 'Tipo de Unidade' definido.");
            }
            return taxaCondominioValorRepository.findByUnidadeTipoUtiCodAndTipoCobrancaTicCod(unidade.getUnidadeTipo().getUtiCod(), tipoCobranca.getTicCod())
                    .map(TaxaCondominioValor::getTcvValor)
                    .orElse(tipoCobranca.getTicValor());
        } else {
            return valorOpcional.orElse(tipoCobranca.getTicValor());
        }
    }

    private FinanceiroCobranca criarRegistroDeCobranca(Unidade unidade, TipoCobranca tipoCobranca, BigDecimal valor, LocalDate dataVencimento) {
        if (valor == null) {
            throw new IllegalStateException("O valor final da cobrança não pôde ser determinado.");
        }

        FinanceiroCobranca novaCobranca = new FinanceiroCobranca();
        novaCobranca.setUnidade(unidade);
        novaCobranca.setTipoCobranca(tipoCobranca);
        novaCobranca.setFicValorTaxa(valor);
        novaCobranca.setFicDtVencimento(dataVencimento);
        novaCobranca.setFicStatusPagamento(CobrancaStatus.A_VENCER);
        novaCobranca.setFicDtCadastro(LocalDateTime.now());

        return financeiroCobrancaRepository.save(novaCobranca);
    }
    
    @Transactional(readOnly = true)
    public Optional<FinanceiroCobranca> buscarCobrancaPorId(Integer id) {
        Optional<FinanceiroCobranca> cobrancaOpt = financeiroCobrancaRepository.findById(id);
        cobrancaOpt.ifPresent(this::checkPermissionToViewCobranca);
        return cobrancaOpt;
    }
    
    @Transactional(readOnly = true)
    public List<FinanceiroCobranca> listarTodasCobrancas(Integer condominioId, Integer unidadeId, String status, LocalDate dataVencimentoInicio, LocalDate dataVencimentoFim) {
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
                 throw new AccessDeniedException("Acesso negado para listar cobranças deste condomínio.");
            }
        }
        
        List<FinanceiroCobranca> cobrancasFiltradas = financeiroCobrancaRepository.findByUnidade_CondominioIn(condominiosAcessiveis);
        Stream<FinanceiroCobranca> filteredStream = cobrancasFiltradas.stream();

        if (unidadeId != null) {
            filteredStream = filteredStream.filter(c -> c.getUnidade().getUniCod().equals(unidadeId));
        }
        if (status != null && !status.isEmpty()) {
            try {
                CobrancaStatus cobrancaStatus = CobrancaStatus.valueOf(status.toUpperCase());
                filteredStream = filteredStream.filter(c -> c.getFicStatusPagamento() == cobrancaStatus);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Status de cobrança inválido: " + status);
            }
        }
        if (dataVencimentoInicio != null && dataVencimentoFim != null) {
            filteredStream = filteredStream.filter(c -> 
                (c.getFicDtVencimento().isAfter(dataVencimentoInicio.minusDays(1)) && c.getFicDtVencimento().isBefore(dataVencimentoFim.plusDays(1)))
            );
        } else if (dataVencimentoInicio != null) {
            filteredStream = filteredStream.filter(c -> c.getFicDtVencimento().isAfter(dataVencimentoInicio.minusDays(1)));
        } else if (dataVencimentoFim != null) {
            filteredStream = filteredStream.filter(c -> c.getFicDtVencimento().isBefore(dataVencimentoFim.plusDays(1)));
        }

        return filteredStream.collect(Collectors.toList());
    }

    @Transactional
    public FinanceiroCobranca atualizarCobranca(Integer id, FinanceiroCobrancaRequestDTO requestDTO) {
        FinanceiroCobranca cobrancaExistente = financeiroCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada com o ID: " + id));
        
        internalCheckPermissionToManageCobranca(cobrancaExistente.getUnidade().getCondominio().getConCod());

        if (requestDTO.uniCod() != null && !requestDTO.uniCod().equals(cobrancaExistente.getUnidade().getUniCod()) ||
            requestDTO.ticCod() != null && !requestDTO.ticCod().equals(cobrancaExistente.getTipoCobranca().getTicCod())) {
             throw new IllegalArgumentException("Não é permitido alterar a Unidade ou o Tipo de Cobrança de uma cobrança existente.");
        }

        if (requestDTO.ficValorTaxa() != null) {
            cobrancaExistente.setFicValorTaxa(requestDTO.ficValorTaxa());
        }
        if (requestDTO.ficDtVencimento() != null) {
            cobrancaExistente.setFicDtVencimento(requestDTO.ficDtVencimento());
        }
        if (requestDTO.ficStatusPagamento() != null) {
            cobrancaExistente.setFicStatusPagamento(requestDTO.ficStatusPagamento());
        }
        if (requestDTO.ficDtPagamento() != null) {
            cobrancaExistente.setFicDtPagamento(requestDTO.ficDtPagamento());
        }
        if (requestDTO.ficValorPago() != null) {
            cobrancaExistente.setFicValorPago(requestDTO.ficValorPago());
        }

        cobrancaExistente.setFicDtAtualizacao(LocalDateTime.now());
        
        return financeiroCobrancaRepository.save(cobrancaExistente);
    }

    @Transactional
    public void deletarCobranca(Integer id) {
        FinanceiroCobranca cobranca = financeiroCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada para exclusão com o ID: " + id));
        
        internalCheckPermissionToManageCobranca(cobranca.getUnidade().getCondominio().getConCod());

        if (cobranca.getFicStatusPagamento() == CobrancaStatus.PAGA || cobranca.getFicStatusPagamento() == CobrancaStatus.CANCELADA) {
            throw new IllegalArgumentException("Não é possível excluir uma cobrança que já foi PAGA ou CANCELADA.");
        }

        financeiroCobrancaRepository.delete(cobranca);
    }
    
    public boolean hasPermissionToManageCobrancaByUnidadeId(Integer unidadeId) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + unidadeId));
        return hasPermissionToManageCobrancaByCondominioId(unidade.getCondominio().getConCod());
    }

    public boolean hasPermissionToManageCobrancaByCobrancaId(Integer cobrancaId) {
        FinanceiroCobranca cobranca = financeiroCobrancaRepository.findById(cobrancaId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada com o ID: " + cobrancaId));
        return hasPermissionToManageCobrancaByCondominioId(cobranca.getUnidade().getCondominio().getConCod());
    }

    public boolean hasPermissionToManageCobrancaByCondominioId(Integer condominioId) {
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

    private void internalCheckPermissionToManageCobranca(Integer condominioId) {
        if (!hasPermissionToManageCobrancaByCondominioId(condominioId)) {
            throw new AccessDeniedException("Acesso negado. Você não tem permissão para gerenciar cobranças neste condomínio.");
        }
    }

    private void checkPermissionToViewCobranca(FinanceiroCobranca cobranca) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) return;

        Integer condominioId = cobranca.getUnidade().getCondominio().getConCod();
        boolean hasAccess = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_", "ROLE_MORADOR_", "ROLE_FUNCIONARIO_ADM_", "ROLE_PORTEIRO_")
                             .contains(condominioId);
        
        if (!hasAccess) {
            throw new AccessDeniedException("Acesso negado para visualizar esta cobrança.");
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