package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Morador;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoradorRepository extends JpaRepository<Morador, Integer> {
    
    List<Morador> findByPessoa(Pessoa pessoa);
   

    List<Morador> findByUnidade(Unidade unidade);
    
    Optional<Morador> findByPessoaAndUnidade(Pessoa pessoa, Unidade unidade);
}