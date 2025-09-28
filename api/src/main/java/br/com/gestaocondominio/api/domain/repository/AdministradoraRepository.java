package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Administradora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdministradoraRepository extends JpaRepository<Administradora, Integer> {
    
    List<Administradora> findByAdmAtivo(Boolean admAtivo);

    @Query("SELECT a FROM Administradora a LEFT JOIN FETCH a.dadosEmpresa LEFT JOIN FETCH a.responsavel")
    List<Administradora> findAllWithPessoas();
}