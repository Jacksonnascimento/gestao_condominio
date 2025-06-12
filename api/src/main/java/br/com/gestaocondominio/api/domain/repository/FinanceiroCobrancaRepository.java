package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import br.com.gestaocondominio.api.domain.enums.CobrancaStatus; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FinanceiroCobrancaRepository extends JpaRepository<FinanceiroCobranca, Integer> {
    List<FinanceiroCobranca> findByUnidadeAndTipoCobrancaAndFicDtVencimentoBetween(
            Unidade unidade,
            TipoCobranca tipoCobranca,
            LocalDate startOfMonth,
            LocalDate endOfMonth);

    List<FinanceiroCobranca> findByTipoCobranca(TipoCobranca tipoCobranca);

    List<FinanceiroCobranca> findByTipoCobrancaAndFicStatusPagamentoNotIn(
            TipoCobranca tipoCobranca,
            List<CobrancaStatus> ficStatusPagamento);

    List<FinanceiroCobranca> findByUnidadeAndFicStatusPagamentoNotIn(
            Unidade unidade,
            List<CobrancaStatus> ficStatusPagamento);
}