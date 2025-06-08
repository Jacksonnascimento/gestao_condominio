package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.AssembleiaTopico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssembleiaTopicoRepository extends JpaRepository<AssembleiaTopico, Integer> {
}