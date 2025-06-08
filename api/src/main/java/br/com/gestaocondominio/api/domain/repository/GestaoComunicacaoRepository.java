package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.GestaoComunicacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GestaoComunicacaoRepository extends JpaRepository<GestaoComunicacao, Integer> {
}