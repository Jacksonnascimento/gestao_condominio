package br.com.gestaocondominio.api.security;

import br.com.gestaocondominio.api.domain.entity.AdministradoraUsuario;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.entity.UsuarioCondominio;
import br.com.gestaocondominio.api.domain.repository.AdministradoraUsuarioRepository;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import br.com.gestaocondominio.api.domain.repository.UsuarioCondominioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final PessoaRepository pessoaRepository;
    private final UsuarioCondominioRepository usuarioCondominioRepository;
    private final AdministradoraUsuarioRepository administradoraUsuarioRepository;

    public UserDetailsServiceImpl(PessoaRepository pessoaRepository,
                                  UsuarioCondominioRepository usuarioCondominioRepository,
                                  AdministradoraUsuarioRepository administradoraUsuarioRepository) {
        this.pessoaRepository = pessoaRepository;
        this.usuarioCondominioRepository = usuarioCondominioRepository;
        this.administradoraUsuarioRepository = administradoraUsuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        Pessoa pessoa = pessoaRepository.findByPesEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();

      
        List<UsuarioCondominio> rolesCondominio = usuarioCondominioRepository.findByPessoa(pessoa);
        for (UsuarioCondominio uc : rolesCondominio) {
            if (uc.getUscAtivoAssociacao()) { 
               
                String authorityString = "ROLE_" + uc.getUscPapel().name() + "_" + uc.getConCod();
                authorities.add(new SimpleGrantedAuthority(authorityString));
            }
        }
        
       
        List<AdministradoraUsuario> rolesAdministradora = administradoraUsuarioRepository.findByPessoa(pessoa);
        for(AdministradoraUsuario au : rolesAdministradora) {
            if (au.getAduAtivo()) { 
                
                String authorityString = "ROLE_" + au.getAduPapel().name() + "_ADMINISTRADORA_" + au.getAdministradora().getAdmCod();
                authorities.add(new SimpleGrantedAuthority(authorityString));
            }
        }

       
        return new UserDetailsImpl(pessoa, authorities);
    }
}