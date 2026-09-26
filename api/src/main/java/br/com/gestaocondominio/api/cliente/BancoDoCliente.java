package br.com.gestaocondominio.api.cliente;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Conexão com o banco do cliente atual. Cada cliente tem o seu pool de conexões, e a escolha é feita a cada conexão
 * pedida, pelo que estiver em {@link ClienteAtual}.
 *
 * <p>Durante a subida, o Hibernate lê do banco a versão do PostgreSQL antes de qualquer cliente estar definido; para
 * isso vale o banco do primeiro cliente, e só isso. Terminada a subida, pedir conexão sem cliente é defeito — o
 * trabalho iria parar no banco errado —, e a chamada é recusada em vez de cair num banco qualquer.</p>
 */
public class BancoDoCliente extends AbstractRoutingDataSource {

    private final String clienteDaSubida;
    private volatile boolean subidaConcluida;

    public BancoDoCliente(Map<Object, Object> bancosPorCliente, String clienteDaSubida) {
        this.clienteDaSubida = clienteDaSubida;
        setTargetDataSources(bancosPorCliente);
        setLenientFallback(false);
    }

    void concluirSubida() {
        subidaConcluida = true;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String identificador = ClienteAtual.identificador();
        if (identificador != null) {
            return identificador;
        }
        if (!subidaConcluida) {
            return clienteDaSubida;
        }
        throw new IllegalStateException("Acesso ao banco sem cliente definido. Numa instalação com vários "
                + "clientes, todo trabalho fora de requisição precisa rodar em ClienteAtual.executar.");
    }

    /** Banco de cada cliente, para as migrações. */
    public DataSource bancoDe(Cliente cliente) {
        return getResolvedDataSources().get(cliente.identificador());
    }
}
