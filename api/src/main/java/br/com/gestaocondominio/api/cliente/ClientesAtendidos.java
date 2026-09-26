package br.com.gestaocondominio.api.cliente;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Clientes atendidos por esta instalação, lidos da variável {@code CLIENTES}.
 *
 * <p>Uma instalação atende vários clientes com os mesmos contêineres: cada cliente tem o seu banco, e a API escolhe o
 * banco pelo endereço acessado. A variável lista os clientes separados por vírgula, cada um no formato
 * {@code identificador:banco}, com domínios adicionais opcionais depois do banco:</p>
 *
 * <pre>
 * CLIENTES=residencialflores:condigtal_residencial_flores,adminsul:condigtal_adminsul
 * CLIENTES=modelo:condigtal_modelo:localhost,exemplo:condigtal_exemplo
 * </pre>
 *
 * <p>O identificador é o subdomínio do cliente: {@code residencialflores.condigtal.com.br} (sistema) e
 * {@code api.residencialflores.condigtal.com.br} (API, usada também pelo aplicativo) levam ao mesmo cliente. Os
 * domínios adicionais cobrem um domínio próprio e o {@code localhost} do desenvolvimento.</p>
 *
 * <p>Sem a variável, a instalação atende um cliente só, no banco de {@code spring.datasource.url}. Com ela, só os
 * clientes listados são atendidos: um endereço que não leva a nenhum é recusado, e a API nunca tenta abrir um banco
 * com um nome vindo da requisição.</p>
 */
@Component
public class ClientesAtendidos {

    /** Prefixo do endereço da API, que chega ao mesmo cliente que o sistema. */
    private static final List<String> PREFIXOS = List.of("api.");

    private static final Pattern IDENTIFICADOR = Pattern.compile("[a-z0-9][a-z0-9-]*");
    private static final Pattern BANCO = Pattern.compile("[a-z0-9_]+");

    private final List<Cliente> clientes;

    public ClientesAtendidos(@Value("${condigtal.clientes:}") String configuracao) {
        this.clientes = ler(configuracao);
    }

    /** Se a instalação atende mais de um cliente, com um banco para cada. */
    public boolean multiplos() {
        return !clientes.isEmpty();
    }

    public List<Cliente> todos() {
        return clientes;
    }

    /**
     * Cliente do endereço acessado, com ou sem porta e com ou sem o prefixo {@code api.}. Vazio quando o endereço não
     * leva a nenhum cliente listado.
     */
    public Optional<Cliente> doEndereco(String endereco) {
        String host = semPorta(endereco);
        if (host.isEmpty()) {
            return Optional.empty();
        }
        String semPrefixo = semPrefixo(host);
        for (Cliente cliente : clientes) {
            if (cliente.dominios().contains(host) || cliente.dominios().contains(semPrefixo)) {
                return Optional.of(cliente);
            }
        }
        String subdominio = semPrefixo.contains(".") ? semPrefixo.substring(0, semPrefixo.indexOf('.')) : "";
        return clientes.stream().filter(c -> c.identificador().equals(subdominio)).findFirst();
    }

    public Optional<Cliente> doIdentificador(String identificador) {
        return clientes.stream().filter(c -> c.identificador().equals(identificador)).findFirst();
    }

    private static String semPorta(String endereco) {
        String host = endereco == null ? "" : endereco.trim().toLowerCase(Locale.ROOT);
        int porta = host.lastIndexOf(':');
        if (porta > 0 && !host.endsWith("]")) {
            host = host.substring(0, porta);
        }
        return host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
    }

    private static String semPrefixo(String host) {
        for (String prefixo : PREFIXOS) {
            if (host.startsWith(prefixo)) {
                return host.substring(prefixo.length());
            }
        }
        return host;
    }

    private static List<Cliente> ler(String configuracao) {
        List<Cliente> lidos = new ArrayList<>();
        if (configuracao == null || configuracao.isBlank()) {
            return lidos;
        }
        Set<String> identificadores = new HashSet<>();
        Set<String> bancos = new HashSet<>();
        Set<String> dominios = new HashSet<>();
        for (String item : configuracao.split(",")) {
            if (item.isBlank()) {
                continue;
            }
            String[] partes = item.trim().toLowerCase(Locale.ROOT).split(":");
            if (partes.length < 2 || !IDENTIFICADOR.matcher(partes[0]).matches() || !BANCO.matcher(partes[1]).matches()) {
                throw new IllegalStateException("Cliente mal escrito em CLIENTES: \"" + item.trim() + "\". Use "
                        + "identificador:banco, com letras minúsculas, números, hífen no identificador e sublinhado "
                        + "no banco, por exemplo residencialflores:condigtal_residencial_flores.");
            }
            List<String> adicionais = Arrays.stream(partes, 2, partes.length).map(String::trim)
                    .filter(d -> !d.isEmpty()).toList();
            if (!identificadores.add(partes[0])) {
                throw new IllegalStateException("O cliente " + partes[0] + " aparece mais de uma vez em CLIENTES.");
            }
            if (!bancos.add(partes[1])) {
                throw new IllegalStateException("O banco " + partes[1] + " aparece em mais de um cliente em "
                        + "CLIENTES. Cada cliente precisa do seu próprio banco.");
            }
            for (String dominio : adicionais) {
                if (!dominios.add(dominio)) {
                    throw new IllegalStateException("O domínio " + dominio + " aparece em mais de um cliente em "
                            + "CLIENTES.");
                }
            }
            lidos.add(new Cliente(partes[0], partes[1], adicionais));
        }
        return List.copyOf(lidos);
    }
}
