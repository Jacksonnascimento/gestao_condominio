package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PessoaRepository extends JpaRepository<Pessoa, Integer> {

    Optional<Pessoa> findByPesCpfCnpj(String pesCpfCnpj);

    Optional<Pessoa> findByPesEmail(String pesEmail);
}