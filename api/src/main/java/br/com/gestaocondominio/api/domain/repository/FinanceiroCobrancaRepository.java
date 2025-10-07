package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.FinanceiroCobranca;
import br.com.gestaocondominio.api.domain.entity.TipoCobranca;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import br.com.gestaocondominio.api.domain.enums.CobrancaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface FinanceiroCobrancaRepository extends JpaRepository<FinanceiroCobranca, Integer> {
    List<FinanceiroCobranca> findByUnidade_CondominioAndFicStatusPagamentoAndFicDtPagamentoBetween(Condominio condominio, CobrancaStatus status, LocalDate dataInicio, LocalDate dataFim);
    List<FinanceiroCobranca> findByTipoCobrancaAndFicStatusPagamentoNotIn(TipoCobranca tipoCobranca, Collection<CobrancaStatus> status);
    List<FinanceiroCobranca> findByUnidadeAndFicStatusPagamentoNotIn(Unidade unidade, Collection<CobrancaStatus> ficStatusPagamento);
    @Query("SELECT f FROM FinanceiroCobranca f WHERE f.unidade.uniCod = :unidadeId AND f.tipoCobranca.ticCod = :tipoCobrancaId AND f.ficDtVencimento BETWEEN :inicioMes AND :fimMes")
    List<FinanceiroCobranca> findByCompetencia(@Param("unidadeId") Integer unidadeId, @Param("tipoCobrancaId") Integer tipoCobrancaId, @Param("inicioMes") LocalDate inicioMes, @Param("fimMes") LocalDate fimMes);
    List<FinanceiroCobranca> findByUnidade_CondominioIn(List<Condominio> condominios);
    List<FinanceiroCobranca> findAllByFicStatusPagamento(CobrancaStatus ficStatusPagamento);
}