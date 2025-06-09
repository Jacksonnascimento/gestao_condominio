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

        if (cobranca.getFicValorTaxa() == null || cobranca.getFicValorTaxa().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da taxa deve ser maior que zero.");
        }
        if (cobranca.getFicDtVencimento() == null) {
            throw new IllegalArgumentException("Data de vencimento da cobrança deve ser informada.");
        }

        if (cobranca.getFicStatusPagamento() == null || cobranca.getFicStatusPagamento().trim().isEmpty()) {
            cobranca.setFicStatusPagamento("A_VENCER");
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

        if (cobrancaAtualizada.getUnidade() != null && !cobrancaAtualizada.getUnidade().getUniCod().equals(cobrancaExistente.getUnidade().getUniCod())) {
             throw new IllegalArgumentException("Não é permitido alterar a Unidade de uma cobrança existente.");
        }
        if (cobrancaAtualizada.getTipoCobranca() != null && !cobrancaAtualizada.getTipoCobranca().getTicCod().equals(cobrancaExistente.getTipoCobranca().getTicCod())) {
             throw new IllegalArgumentException("Não é permitido alterar o Tipo de Cobrança de uma cobrança existente.");
        }

        cobrancaExistente.setFicValorTaxa(cobrancaAtualizada.getFicValorTaxa());
        cobrancaExistente.setFicDtVencimento(cobrancaAtualizada.getFicDtVencimento());
        cobrancaExistente.setFicDtPagamento(cobrancaAtualizada.getFicDtPagamento());
        cobrancaExistente.setFicValorPago(cobrancaAtualizada.getFicValorPago());
        cobrancaExistente.setFicStatusPagamento(cobrancaAtualizada.getFicStatusPagamento());

        cobrancaExistente.setFicDtAtualizacao(LocalDateTime.now());
        return financeiroCobrancaRepository.save(cobrancaExistente);
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
        for (Unidade unidade : unidadesDoCondominio) {
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

        return novasCobrancas;
    }
}