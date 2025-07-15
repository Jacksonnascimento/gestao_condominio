package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.DespesaCategoria;
import br.com.gestaocondominio.api.domain.entity.FinanceiroDespesa;
import br.com.gestaocondominio.api.domain.enums.DespesaStatusPagamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FinanceiroDespesaRepository extends JpaRepository<FinanceiroDespesa, Integer> {
    List<FinanceiroDespesa> findByCondominioAndDesDataVencimentoBetween(Condominio condominio, LocalDate startDate,
            LocalDate endDate);

    List<FinanceiroDespesa> findByCondominioAndCategoriaAndDesDataVencimentoBetween(Condominio condominio,
            DespesaCategoria categoria, LocalDate startDate, LocalDate endDate);

    List<FinanceiroDespesa> findByCondominioIn(List<Condominio> condominios);

    List<FinanceiroDespesa> findByCondominioAndDesStatusPagamentoAndDesDataPagamentoBetween(
            Condominio condominio, DespesaStatusPagamento status, LocalDate dataInicio, LocalDate dataFim);
}