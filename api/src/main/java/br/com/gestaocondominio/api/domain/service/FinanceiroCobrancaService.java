package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.*;
import br.com.gestaocondominio.api.domain.enums.CobrancaStatus;
import br.com.gestaocondominio.api.domain.repository.*;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FinanceiroCobrancaService {

    private final FinanceiroCobrancaRepository financeiroCobrancaRepository;
    private final UnidadeRepository unidadeRepository;
    private final TipoCobrancaRepository tipoCobrancaRepository;
    private final CondominioRepository condominioRepository;
    private final MoradorRepository moradorRepository;

    public FinanceiroCobrancaService(FinanceiroCobrancaRepository financeiroCobrancaRepository,
            UnidadeRepository unidadeRepository, TipoCobrancaRepository tipoCobrancaRepository,
            CondominioRepository condominioRepository, MoradorRepository moradorRepository) {
        this.financeiroCobrancaRepository = financeiroCobrancaRepository;
        this.unidadeRepository = unidadeRepository;
        this.tipoCobrancaRepository = tipoCobrancaRepository;
        this.condominioRepository = condominioRepository;
        this.moradorRepository = moradorRepository;
    }

    @Transactional
    public FinanceiroCobranca cadastrarCobranca(FinanceiroCobranca cobranca) {
        if (cobranca.getUnidade() == null || cobranca.getUnidade().getUniCod() == null) {
            throw new IllegalArgumentException("Unidade deve ser informada para a cobrança.");
        }
        Unidade unidade = unidadeRepository.findById(cobranca.getUnidade().getUniCod())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unidade não encontrada com o ID: " + cobranca.getUnidade().getUniCod()));

        checkAdminOrSindicoPermission(unidade.getCondominio().getConCod());

        if (cobranca.getTipoCobranca() == null || cobranca.getTipoCobranca().getTicCod() == null) {
            throw new IllegalArgumentException("Tipo de cobrança deve ser informado.");
        }
        tipoCobrancaRepository.findById(cobranca.getTipoCobranca().getTicCod())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de cobrança não encontrado com o ID: " + cobranca.getTipoCobranca().getTicCod()));

        if (cobranca.getFicValorTaxa() == null || cobranca.getFicValorTaxa().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor da taxa não pode ser nulo ou negativo.");
        }
        if (cobranca.getFicDtVencimento() == null) {
            throw new IllegalArgumentException("Data de vencimento da cobrança deve ser informada.");
        }

        if (cobranca.getFicStatusPagamento() == null) {
            cobranca.setFicStatusPagamento(CobrancaStatus.A_VENCER);
        }

        cobranca.setFicDtCadastro(LocalDateTime.now());
        cobranca.setFicDtAtualizacao(LocalDateTime.now());

        return financeiroCobrancaRepository.save(cobranca);
    }

    public List<FinanceiroCobranca> listarTodasCobrancas() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN")) {
            return financeiroCobrancaRepository.findAll();
        }

        Set<Integer> condoIdsComAcessoAdmin = getCondoIdsFromRoles(authentication, "ROLE_SINDICO_", "ROLE_ADMIN_");
        if (!condoIdsComAcessoAdmin.isEmpty()) {
            List<Condominio> condominios = condominioRepository.findAllById(condoIdsComAcessoAdmin);
            List<Unidade> unidades = unidadeRepository.findByCondominioIn(condominios);
            return financeiroCobrancaRepository.findByUnidadeIn(unidades);
        }

        List<Morador> vinculosMorador = moradorRepository.findByPessoa(userDetails.getPessoa());
        List<Unidade> unidadesDoMorador = vinculosMorador.stream().map(Morador::getUnidade)
                .collect(Collectors.toList());
        if (unidadesDoMorador.isEmpty()) {
            return List.of();
        }
        return financeiroCobrancaRepository.findByUnidadeIn(unidadesDoMorador);
    }

    public Optional<FinanceiroCobranca> buscarCobrancaPorId(Integer id) {
        Optional<FinanceiroCobranca> cobrancaOpt = financeiroCobrancaRepository.findById(id);
        cobrancaOpt.ifPresent(this::checkPermissionToViewCobranca);
        return cobrancaOpt;
    }

    @Transactional
    public FinanceiroCobranca atualizarCobranca(Integer id, FinanceiroCobranca cobrancaAtualizada) {
        FinanceiroCobranca cobrancaExistente = financeiroCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada com o ID: " + id));

        checkAdminOrSindicoPermission(cobrancaExistente.getUnidade().getCondominio().getConCod());

        if (cobrancaAtualizada.getFicValorTaxa() != null) {
            cobrancaExistente.setFicValorTaxa(cobrancaAtualizada.getFicValorTaxa());
        }
        if (cobrancaAtualizada.getFicDtVencimento() != null) {
            cobrancaExistente.setFicDtVencimento(cobrancaAtualizada.getFicDtVencimento());
        }
        if (cobrancaAtualizada.getFicDtPagamento() != null) {
            cobrancaExistente.setFicDtPagamento(cobrancaAtualizada.getFicDtPagamento());
        }
        if (cobrancaAtualizada.getFicValorPago() != null) {
            cobrancaExistente.setFicValorPago(cobrancaAtualizada.getFicValorPago());
        }
        if (cobrancaAtualizada.getFicStatusPagamento() != null) {
            cobrancaExistente.setFicStatusPagamento(cobrancaAtualizada.getFicStatusPagamento());
        }

        cobrancaExistente.setFicDtAtualizacao(LocalDateTime.now());
        return financeiroCobrancaRepository.save(cobrancaExistente);
    }

    @Transactional
    public FinanceiroCobranca cancelarCobranca(Integer id) {
        FinanceiroCobranca cobranca = financeiroCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cobrança não encontrada para cancelamento com o ID: " + id));

        checkAdminOrSindicoPermission(cobranca.getUnidade().getCondominio().getConCod());

        if (CobrancaStatus.PAGA.equals(cobranca.getFicStatusPagamento())) {
            throw new IllegalArgumentException("Não é possível cancelar uma cobrança que já foi PAGA.");
        }
        if (CobrancaStatus.CANCELADA.equals(cobranca.getFicStatusPagamento())) {
            throw new IllegalArgumentException("Cobrança já está com status 'CANCELADA'.");
        }

        cobranca.setFicStatusPagamento(CobrancaStatus.CANCELADA);
        cobranca.setFicDtAtualizacao(LocalDateTime.now());
        return financeiroCobrancaRepository.save(cobranca);
    }

    @Transactional
    public List<FinanceiroCobranca> gerarCobrancasEmLote(Integer condominioId, LocalDate dataVencimento,
            Integer tipoCobrancaId, BigDecimal valorOpcional) {
        Condominio condominioReferencia = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + condominioId));

        TipoCobranca tipoCobranca = tipoCobrancaRepository.findById(tipoCobrancaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de cobrança não encontrado com o ID: " + tipoCobrancaId));

        List<Unidade> unidadesDoCondominio = unidadeRepository.findByCondominio(condominioReferencia);

        if (unidadesDoCondominio.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma unidade encontrada para o condomínio especificado.");
        }

        List<FinanceiroCobranca> novasCobrancas = new ArrayList<>();
        YearMonth mesAnoVencimento = YearMonth.from(dataVencimento);
        LocalDate inicioDoMes = mesAnoVencimento.atDay(1);
        LocalDate fimDoMes = mesAnoVencimento.atEndOfMonth();

        for (Unidade unidade : unidadesDoCondominio) {
            boolean jaExisteCobrancaValida = financeiroCobrancaRepository
                    .findByUnidadeAndTipoCobrancaAndFicDtVencimentoBetween(unidade, tipoCobranca, inicioDoMes, fimDoMes)
                    .stream()
                    .anyMatch(c -> c.getFicStatusPagamento() != CobrancaStatus.CANCELADA);

            if (!jaExisteCobrancaValida) {
                FinanceiroCobranca novaCobranca = new FinanceiroCobranca();
                novaCobranca.setUnidade(unidade);
                novaCobranca.setTipoCobranca(tipoCobranca);

                BigDecimal valorDaCobranca = (valorOpcional != null) ? valorOpcional
                        : unidade.getUniValorTaxaCondominio();
                novaCobranca.setFicValorTaxa(valorDaCobranca);

                novaCobranca.setFicDtVencimento(dataVencimento);
                novaCobranca.setFicStatusPagamento(CobrancaStatus.A_VENCER);
                novaCobranca.setFicDtCadastro(LocalDateTime.now());
                novaCobranca.setFicDtAtualizacao(LocalDateTime.now());
                novasCobrancas.add(financeiroCobrancaRepository.save(novaCobranca));
            }
        }
        return novasCobrancas;
    }

    private void checkAdminOrSindicoPermission(Integer condominioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean hasPermission = hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
                hasAuthority(authentication, "ROLE_SINDICO_" + condominioId) ||
                hasAuthority(authentication, "ROLE_ADMIN_" + condominioId);
        if (!hasPermission) {
            throw new AccessDeniedException(
                    "Acesso negado. Você não tem permissão para gerenciar finanças neste condomínio.");
        }
    }

    private void checkPermissionToViewCobranca(FinanceiroCobranca cobranca) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (hasAuthority(authentication, "ROLE_GLOBAL_ADMIN") ||
                hasAuthority(authentication, "ROLE_SINDICO_" + cobranca.getUnidade().getCondominio().getConCod()) ||
                hasAuthority(authentication, "ROLE_ADMIN_" + cobranca.getUnidade().getCondominio().getConCod())) {
            return;
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        boolean isMoradorDaUnidade = moradorRepository
                .findByPessoaAndUnidade(userDetails.getPessoa(), cobranca.getUnidade()).isPresent();
        if (!isMoradorDaUnidade) {
            throw new AccessDeniedException("Acesso negado. Você não tem permissão para visualizar esta cobrança.");
        }
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }

    private Set<Integer> getCondoIdsFromRoles(Authentication auth, String... prefixes) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authString -> Arrays.stream(prefixes).anyMatch(authString::startsWith))
                .map(authString -> Integer.parseInt(authString.substring(authString.lastIndexOf('_') + 1)))
                .collect(Collectors.toSet());
    }
}