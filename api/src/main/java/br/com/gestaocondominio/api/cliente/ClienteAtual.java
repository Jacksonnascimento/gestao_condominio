package br.com.gestaocondominio.api.cliente;

import java.util.function.Supplier;

/**
 * Cliente cujos dados a linha de execução atual está lendo e gravando.
 *
 * <p>Numa requisição, quem o define é o {@link FiltroDoCliente}, pelo endereço acessado. Fora de requisição — rotina
 * agendada, subida da API, envio de e-mail em segundo plano — quem roda o trabalho o define por
 * {@link #executar(Cliente, Runnable)}. O banco e a pasta de arquivos seguem o que estiver aqui.</p>
 *
 * <p>Numa instalação de um cliente só não há nada aqui, e tudo funciona como sempre funcionou.</p>
 */
public final class ClienteAtual {

    /** Marcador dos endereços configurados por modelo, como {@code https://{cliente}.condigtal.com.br}. */
    public static final String MARCADOR = "{cliente}";

    private static final ThreadLocal<Cliente> ATUAL = new ThreadLocal<>();

    private ClienteAtual() {
    }

    /** Cliente atual, ou {@code null} numa instalação de cliente único ou fora de qualquer cliente. */
    public static Cliente obter() {
        return ATUAL.get();
    }

    public static String identificador() {
        Cliente cliente = ATUAL.get();
        return cliente == null ? null : cliente.identificador();
    }

    /** Roda o trabalho com os dados do cliente e, ao terminar, volta ao cliente que estava antes. */
    public static void executar(Cliente cliente, Runnable trabalho) {
        executar(cliente, () -> {
            trabalho.run();
            return null;
        });
    }

    public static <T> T executar(Cliente cliente, Supplier<T> trabalho) {
        Cliente anterior = ATUAL.get();
        ATUAL.set(cliente);
        try {
            return trabalho.get();
        } finally {
            if (anterior == null) {
                ATUAL.remove();
            } else {
                ATUAL.set(anterior);
            }
        }
    }

    /** Usado pelo filtro, que precisa envolver uma cadeia que lança exceções verificadas. */
    static void definir(Cliente cliente) {
        ATUAL.set(cliente);
    }

    static void limpar() {
        ATUAL.remove();
    }

    /**
     * Endereço configurado com o marcador {@code {cliente}} trocado pelo identificador do cliente atual. Assim uma só
     * variável ({@code WEB_URL_PUBLICA=https://{cliente}.condigtal.com.br}) serve a todos os clientes.
     */
    public static String noEndereco(String modelo) {
        String identificador = identificador();
        if (modelo == null || identificador == null) {
            return modelo;
        }
        return modelo.replace(MARCADOR, identificador);
    }
}
