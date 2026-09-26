package br.com.gestaocondominio.api.cliente;

import br.com.gestaocondominio.api.domain.entity.Pessoa;
import br.com.gestaocondominio.api.domain.repository.PessoaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Primeiro acesso de cada cliente. Num banco sem nenhuma pessoa cadastrada, a subida cria um administrador com os
 * dados de {@code ADMINISTRADOR_INICIAL_*}; com alguém já cadastrado, nada é feito. Sem as variáveis, também não.
 */
@Component
public class AdministradorInicial {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final ClientesAtendidos clientes;
    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;
    private final String cpf;
    private final String nome;

    public AdministradorInicial(ClientesAtendidos clientes, PessoaRepository pessoaRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${condigtal.administrador-inicial.email:}") String email,
                                @Value("${condigtal.administrador-inicial.senha:}") String senha,
                                @Value("${condigtal.administrador-inicial.cpf:}") String cpf,
                                @Value("${condigtal.administrador-inicial.nome:}") String nome) {
        this.clientes = clientes;
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim();
        this.senha = senha;
        this.cpf = cpf.replaceAll("\\D", "");
        this.nome = nome.isBlank() ? "Administrador" : nome.trim();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void criarOndeFaltar() {
        if (email.isEmpty() || senha.isBlank() || cpf.isEmpty()) {
            return;
        }
        if (!clientes.multiplos()) {
            criarSeVazio("instalação");
            return;
        }
        for (Cliente cliente : clientes.todos()) {
            ClienteAtual.executar(cliente, () -> criarSeVazio(cliente.identificador()));
        }
    }

    private void criarSeVazio(String onde) {
        if (pessoaRepository.count() > 0) {
            return;
        }
        Pessoa administrador = new Pessoa();
        administrador.setPesNome(nome);
        administrador.setPesEmail(email);
        administrador.setPesCpfCnpj(cpf);
        administrador.setPesTipo('F');
        administrador.setPesSenhaLogin(passwordEncoder.encode(senha));
        administrador.setPesIsGlobalAdmin(true);
        administrador.setPesAtivo(true);
        administrador.setPesDtCadastro(LocalDateTime.now());
        pessoaRepository.save(administrador);
        log.info("Administrador inicial {} criado ({}).", email, onde);
    }
}
