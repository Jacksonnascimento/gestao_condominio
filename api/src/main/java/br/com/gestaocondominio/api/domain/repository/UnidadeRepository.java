package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadeRepository extends JpaRepository<Unidade, Integer> {
    Optional<Unidade> findByUniNumeroAndCondominio(String uniNumero, Condominio condominio);
    List<Unidade> findByCondominio(Condominio condominio); // <-- Esta linha é crucial
}