package br.com.gestaocondominio.api.domain.service;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import br.com.gestaocondominio.api.domain.repository.FinanceiroCobrancaRepository;
import br.com.gestaocondominio.api.domain.repository.UnidadeRepository;
import br.com.gestaocondominio.api.domain.repository.TipoCobrancaRepository;
import br.com.gestaocondominio.api.domain.repository.CondominioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth; 
import java.util.List;
import java.util.Optional;

@Service
public class FinanceiroCobrancaService {

    private final FinanceiroCobrancaRepository financeiroCobrancaRepository;
    private final UnidadeRepository unidadeRepository;
    private final TipoCobrancaRepository tipoCobrancaRepository;
    private final CondominioRepository condominioRepository;

    public FinanceiroCobrancaService(FinanceiroCobrancaRepository financeiroCobrancaRepository,
                                     UnidadeRepository unidadeRepository,
                                     TipoCobrancaRepository tipoCobrancaRepository,
                                     CondominioRepository condominioRepository) {
        this.financeiroCobrancaRepository = financeiroCobrancaRepository;
        this.unidadeRepository = unidadeRepository;
        this.tipoCobrancaRepository = tipoCobrancaRepository;
        this.condominioRepository = condominioRepository;
    }

    public FinanceiroCobranca cadastrarCobranca(FinanceiroCobranca cobranca) {
        if (cobranca.getUnidade() == null || cobranca.getUnidade().getUniCod() == null) {
            throw new IllegalArgumentException("Unidade deve ser informada para a cobrança.");
        }
        unidadeRepository.findById(cobranca.getUnidade().getUniCod())
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada com o ID: " + cobranca.getUnidade().getUniCod()));

        if (cobranca.getTipoCobranca() == null || cobranca.getTipoCobranca().getTicCod() == null) {
            throw new IllegalArgumentException("Tipo de cobrança deve ser informado.");
        }
        tipoCobrancaRepository.findById(cobranca.getTipoCobranca().getTicCod())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cobrança não encontrado com o ID: " + cobranca.getTipoCobranca().getTicCod()));

        if (cobranca.getFicValorTaxa() == null || cobranca.getFicValorTaxa().compareTo(BigDecimal.ZERO) < 0) {
             throw new IllegalArgumentException("Valor da taxa não pode ser nulo ou negativo.");
        }
        if (cobranca.getFicDtVencimento() == null) {
            throw new IllegalArgumentException("Data de vencimento da cobrança deve ser informada.");
        }

        if (cobranca.getFicStatusPagamento() == null || cobranca.getFicStatusPagamento().trim().isEmpty()) {
            cobranca.setFicStatusPagamento("A_VENCER");
        }
        String statusInicial = cobranca.getFicStatusPagamento().toUpperCase();
        if ("PAGA".equals(statusInicial) || "CANCELADA".equals(statusInicial)) {
            throw new IllegalArgumentException("Não é permitido cadastrar uma cobrança com status inicial '" + statusInicial + "'.");
        }

        cobranca.setFicDtCadastro(LocalDateTime.now());
        cobranca.setFicDtAtualizacao(LocalDateTime.now());

        return financeiroCobrancaRepository.save(cobranca);
    }

    public Optional<FinanceiroCobranca> buscarCobrancaPorId(Integer id) {
        return financeiroCobrancaRepository.findById(id);
    }

    public List<FinanceiroCobranca> listarTodasCobrancas() {
        return financeiroCobrancaRepository.findAll();
    }

    @Transactional
    public FinanceiroCobranca atualizarCobranca(Integer id, FinanceiroCobranca cobrancaAtualizada) {
        FinanceiroCobranca cobrancaExistente = financeiroCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada com o ID: " + id));

        if ("PAGA".equalsIgnoreCase(cobrancaExistente.getFicStatusPagamento()) || "CANCELADA".equalsIgnoreCase(cobrancaExistente.getFicStatusPagamento())) {
            throw new IllegalArgumentException("Não é possível atualizar uma cobrança com status '" + cobrancaExistente.getFicStatusPagamento() + "'.");
        }

        if (cobrancaAtualizada.getUnidade() != null && !cobrancaAtualizada.getUnidade().getUniCod().equals(cobrancaExistente.getUnidade().getUniCod())) {
             throw new IllegalArgumentException("Não é permitido alterar a Unidade de uma cobrança existente.");
        }
        if (cobrancaAtualizada.getTipoCobranca() != null && !cobrancaAtualizada.getTipoCobranca().getTicCod().equals(cobrancaExistente.getTipoCobranca().getTicCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Tipo de Cobrança de uma cobrança existente.");
        }
        
        if (cobrancaAtualizada.getFicValorTaxa() != null && cobrancaAtualizada.getFicValorTaxa().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor da taxa não pode ser nulo ou negativo na atualização.");
        }
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
        
        if (cobrancaAtualizada.getFicStatusPagamento() != null && !cobrancaAtualizada.getFicStatusPagamento().trim().isEmpty()) {
            String novoStatus = cobrancaAtualizada.getFicStatusPagamento().toUpperCase();
            String statusAtual = cobrancaExistente.getFicStatusPagamento().toUpperCase();

            switch (statusAtual) {
                case "A_VENCER":
                    if (!("PAGA".equals(novoStatus) || "VENCIDA".equals(novoStatus) || "CANCELADA".equals(novoStatus))) {
                        throw new IllegalArgumentException("Transição de status inválida de 'A_VENCER' para '" + novoStatus + "'.");
                    }
                    break;
                case "VENCIDA":
                    if (!("PAGA".equals(novoStatus) || "CANCELADA".equals(novoStatus))) {
                        throw new IllegalArgumentException("Transição de status inválida de 'VENCIDA' para '" + novoStatus + "'.");
                    }
                    break;
                case "PAGA":
                case "CANCELADA":
                    throw new IllegalArgumentException("Status final '" + statusAtual + "' não pode ser alterado.");
                default:
                    throw new IllegalArgumentException("Status atual desconhecido: " + statusAtual + ".");
            }
            cobrancaExistente.setFicStatusPagamento(novoStatus);
        }

        cobrancaExistente.setFicDtAtualizacao(LocalDateTime.now());
        return financeiroCobrancaRepository.save(cobrancaExistente);
    }

    public FinanceiroCobranca cancelarCobranca(Integer id) {
        FinanceiroCobranca cobranca = financeiroCobrancaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada para cancelamento com o ID: " + id));

        String statusAtual = cobranca.getFicStatusPagamento().toUpperCase();

        if ("PAGA".equals(statusAtual)) {
            throw new IllegalArgumentException("Não é possível cancelar uma cobrança que já foi PAGA.");
        }
        if ("CANCELADA".equals(statusAtual)) {
            throw new IllegalArgumentException("Cobrança já está com status 'CANCELADA'.");
        }
        
        cobranca.setFicStatusPagamento("CANCELADA");
        cobranca.setFicDtAtualizacao(LocalDateTime.now());
        return financeiroCobrancaRepository.save(cobranca);
    }

    @Transactional
    public List<FinanceiroCobranca> gerarCobrancasEmLote(Integer condominioId, LocalDate dataVencimento, Integer tipoCobrancaId) {
        Condominio condominioReferencia = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new IllegalArgumentException("Condomínio não encontrado com o ID: " + condominioId));

        TipoCobranca tipoCobranca = tipoCobrancaRepository.findById(tipoCobrancaId)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de cobrança não encontrado com o ID: " + tipoCobrancaId));

        List<Unidade> unidadesDoCondominio = unidadeRepository.findByCondominio(condominioReferencia);

        if (unidadesDoCondominio.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma unidade encontrada para o condomínio especificado.");
        }

        List<FinanceiroCobranca> novasCobrancas = new java.util.ArrayList<>();
        YearMonth mesAnoVencimento = YearMonth.from(dataVencimento);
        LocalDate inicioDoMes = mesAnoVencimento.atDay(1);
        LocalDate fimDoMes = mesAnoVencimento.atEndOfMonth();


        for (Unidade unidade : unidadesDoCondominio) {
            
            List<FinanceiroCobranca> cobrancasExistentesNoPeriodo = financeiroCobrancaRepository.findByUnidadeAndTipoCobrancaAndFicDtVencimentoBetween(
                unidade, tipoCobranca, inicioDoMes, fimDoMes
            );

            boolean deveGerarNovaCobranca = true;
            for (FinanceiroCobranca cobrancaExistente : cobrancasExistentesNoPeriodo) {
                String statusExistente = cobrancaExistente.getFicStatusPagamento().toUpperCase();
                if ("PAGA".equals(statusExistente) || "A_VENCER".equals(statusExistente) || "VENCIDA".equals(statusExistente)) {
                    deveGerarNovaCobranca = false; 
                    break;
                }
                
            }

            if (deveGerarNovaCobranca) {
                FinanceiroCobranca novaCobranca = new FinanceiroCobranca();
                novaCobranca.setUnidade(unidade);
                novaCobranca.setTipoCobranca(tipoCobranca);
                novaCobranca.setFicValorTaxa(unidade.getUniValorTaxaCondominio());
                novaCobranca.setFicDtVencimento(dataVencimento);
                novaCobranca.setFicStatusPagamento("A_VENCER");
                novaCobranca.setFicDtCadastro(LocalDateTime.now());
                novaCobranca.setFicDtAtualizacao(LocalDateTime.now());

                novasCobrancas.add(financeiroCobrancaRepository.save(novaCobranca));
            }
        }

        return novasCobrancas;
    }
}