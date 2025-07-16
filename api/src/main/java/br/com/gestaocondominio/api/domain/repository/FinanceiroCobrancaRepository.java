package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca;
import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.CobrancaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinanceiroCobrancaRepository extends JpaRepository<FinanceiroCobranca, Integer> {

        List<FinanceiroCobranca> findByUnidadeIn(List<Unidade> unidades);

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

        List<FinanceiroCobranca> findByFicStatusPagamentoAndFicDtVencimentoBefore(CobrancaStatus status,
                        LocalDate data);

        List<FinanceiroCobranca> findByUnidade_CondominioAndFicStatusPagamentoAndFicDtPagamentoBetween(
                        Condominio condominio, CobrancaStatus status, LocalDate dataInicio, LocalDate dataFim);

        List<FinanceiroCobranca> findByUnidade_CondominioIn(List<Condominio> condominios);

        @Query("SELECT fc FROM FinanceiroCobranca fc WHERE fc.unidade.uniCod = :unidadeId AND fc.tipoCobranca.ticCod = :tipoCobrancaId AND fc.ficDtVencimento BETWEEN :inicioMes AND :fimMes")
        Optional<FinanceiroCobranca> findByCompetencia(
                        @Param("unidadeId") Integer unidadeId,
                        @Param("tipoCobrancaId") Integer tipoCobrancaId,
                        @Param("inicioMes") LocalDate inicioMes,
                        @Param("fimMes") LocalDate fimMes);

        List<FinanceiroCobranca> findAllByFicStatusPagamento(CobrancaStatus status);
}