package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Condominio;
import br.com.gestaocondominio.api.domain.entity.Pessoa; 
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioCondominioRepository extends JpaRepository<UsuarioCondominio, UsuarioCondominioId> {

    
    List<UsuarioCondominio> findByPessoa(Pessoa pessoa);
 

    List<UsuarioCondominio> findByCondominio(Condominio condominio);

    List<UsuarioCondominio> findByUscAtivoAssociacao(Boolean uscAtivoAssociacao);
}