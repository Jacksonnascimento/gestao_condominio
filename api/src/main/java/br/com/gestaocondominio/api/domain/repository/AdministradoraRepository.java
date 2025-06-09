package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Administradora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; 

@Repository
public interface AdministradoraRepository extends JpaRepository<Administradora, Integer> {
    
    List<Administradora> findByAdmAtivo(Boolean admAtivo);
}