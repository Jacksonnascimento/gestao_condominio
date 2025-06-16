package br.com.gestaocondominio.api.security;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UserDetailsImpl implements UserDetails {

    private final Pessoa pessoa;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Pessoa pessoa, Collection<? extends GrantedAuthority> authorities) {
        this.pessoa = pessoa;
        this.authorities = authorities;
    }
    
 
    public Pessoa getPessoa() {
        return pessoa;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.pessoa.getPesSenhaLogin();
    }

    @Override
    public String getUsername() {
        // username para o login
        return this.pessoa.getPesEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        // No momento, não temos lógica de expiração de conta
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // No momento, não temos lógica de bloqueio de conta
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // No momento, não temos lógica de expiração de credenciais
        return true;
    }

    @Override
    public boolean isEnabled() {
        // A conta está ativa se o campo PES_ATIVO for true
        return this.pessoa.getPesAtivo();
    }
}