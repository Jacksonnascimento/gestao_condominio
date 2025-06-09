package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Morador;
import br.com.gestaocondominio.api.domain.entity.Pessoa; // Importe Pessoa
import br.com.gestaocondominio.api.domain.entity.Unidade; // Importe Unidade
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Importe Optional

@Repository
public interface MoradorRepository extends JpaRepository<Morador, Integer> {
    List<Morador> findByUnidade(Unidade unidade);
    List<Morador> findByPessoa(Pessoa pessoa);

    Optional<Morador> findByPessoaAndUnidade(Pessoa pessoa, Unidade unidade);
}