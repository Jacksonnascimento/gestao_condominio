package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.FinanceiroDespesa;
import br.com.gestaocondominio.api.domain.enums.DespesaStatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface FinanceiroDespesaRepository extends JpaRepository<FinanceiroDespesa, Integer> {
    List<FinanceiroDespesa> findByCondominioAndDesStatusPagamentoAndDesDataPagamentoBetween(Condominio condominio, DespesaStatusPagamento status, LocalDate dataInicio, LocalDate dataFim);
    List<FinanceiroDespesa> findByCondominioIn(List<Condominio> condominios);
}