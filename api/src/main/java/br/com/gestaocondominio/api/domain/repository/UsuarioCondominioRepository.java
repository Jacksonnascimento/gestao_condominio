package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UsuarioCondominioRepository extends JpaRepository<UsuarioCondominio, UsuarioCondominioId> {
 
}