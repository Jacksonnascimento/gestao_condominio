package br.com.gestaocondominio.api.domain.repository;

import br.com.gestaocondominio.api.domain.entity.Administradora;
import br.com.gestaocondominio.api.domain.entity.AdministradoraUsuario;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.enums.AdminCompanyRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdministradoraUsuarioRepository extends JpaRepository<AdministradoraUsuario, Integer> {

   
    List<AdministradoraUsuario> findByPessoa(Pessoa pessoa);
    
    List<AdministradoraUsuario> findByAdministradora(Administradora administradora);

    Optional<AdministradoraUsuario> findByAdministradoraAndPessoaAndAduPapel(
            Administradora administradora,
            Pessoa pessoa,
            AdminCompanyRole aduPapel);

    List<AdministradoraUsuario> findByAduAtivo(Boolean aduAtivo);
}