package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Morador;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; 


@Repository
public interface MoradorRepository extends JpaRepository<Morador, Integer> {

    List<Morador> findByUnidade(Unidade unidade);

    
    List<Morador> findByPessoa(Pessoa pessoa);

    
}