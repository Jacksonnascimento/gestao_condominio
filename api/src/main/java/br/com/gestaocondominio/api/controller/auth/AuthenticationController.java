package br.com.gestaocondominio.api.controller.auth;

import br.com.gestaocondominio.api.controller.dto.LoginRequest;
import br.com.gestaocondominio.api.controller.dto.LoginResponse;
import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.security.JwtService;
import br.com.gestaocondominio.api.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginRequest request) {
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.senha()
                )
        );

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        final String token = jwtService.generateToken(userDetails);
        
        Pessoa pessoa = ((UserDetailsImpl) userDetails).getPessoa();

        return ResponseEntity.ok(new LoginResponse(token, pessoa));
    }
}