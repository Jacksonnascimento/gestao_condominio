package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CondominioRepository extends JpaRepository<Condominio, Integer> {
    
    Optional<Condominio> findByConNome(String conNome);
    
    List<Condominio> findByConGeracaoAutoAtivaIsTrueAndConDiaGeracaoCobranca(int diaDoMes);
    
    List<Condominio> findByConAtivo(Boolean conAtivo);
}