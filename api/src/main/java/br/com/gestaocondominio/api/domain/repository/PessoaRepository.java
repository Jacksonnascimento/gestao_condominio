package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository 
public interface PessoaRepository extends JpaRepository<Pessoa, Integer> {
    
}