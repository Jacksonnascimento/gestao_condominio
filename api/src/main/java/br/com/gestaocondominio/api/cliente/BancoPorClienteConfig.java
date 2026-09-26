package br.com.gestaocondominio.api.cliente;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskDecorator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Banco de dados de cada cliente atendido.
 *
 * <p>Com um cliente só (sem {@code CLIENTES}), o banco é o de {@code spring.datasource.url}. Com vários, o endereço,
 * o usuário e a senha do servidor PostgreSQL continuam vindo de {@code spring.datasource.*}, e só o nome do banco muda
 * de um cliente para outro.</p>
 */
@Configuration
public class BancoPorClienteConfig {

    private static final Logger log = LoggerFactory.getLogger(BancoPorClienteConfig.class);

    /** {@code jdbc:postgresql://servidor:porta/banco?parametros}: o nome do banco é o trecho trocado. */
    private static final Pattern URL_JDBC = Pattern.compile("^(jdbc:[^/]+//[^/]+/)([^?]*)(\\?.*)?$");

    /**
     * O pool de cada cliente vai até {@code conexoesPorCliente} conexões e, parado, encolhe até
     * {@code conexoesOciosas}. Sem o mínimo, o padrão do Hikari deixaria cada cliente com todas as conexões abertas o
     * tempo todo, e o servidor PostgreSQL (100 conexões no padrão) comportaria poucos clientes por instalação.
     */
    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties propriedades, ClientesAtendidos clientes,
                                 @Value("${condigtal.banco.conexoes-por-cliente:10}") int conexoesPorCliente,
                                 @Value("${condigtal.banco.conexoes-ociosas-por-cliente:2}") int conexoesOciosas) {
        if (!clientes.multiplos()) {
            HikariDataSource banco = propriedades.initializeDataSourceBuilder().type(HikariDataSource.class).build();
            dimensionar(banco, conexoesPorCliente, conexoesOciosas);
            return banco;
        }
        Map<Object, Object> bancos = new LinkedHashMap<>();
        for (Cliente cliente : clientes.todos()) {
            HikariDataSource banco = propriedades.initializeDataSourceBuilder().type(HikariDataSource.class)
                    .url(urlDoBanco(propriedades.determineUrl(), cliente.banco()))
                    .build();
            banco.setPoolName("banco-" + cliente.identificador());
            dimensionar(banco, conexoesPorCliente, conexoesOciosas);
            bancos.put(cliente.identificador(), banco);
        }
        log.info("Instalação com {} clientes: {}", bancos.size(), String.join(", ",
                clientes.todos().stream().map(c -> c.identificador() + " (banco " + c.banco() + ")").toList()));
        return new BancoDoCliente(bancos, clientes.todos().get(0).identificador());
    }

    private static void dimensionar(HikariDataSource banco, int maximo, int ociosas) {
        banco.setMaximumPoolSize(Math.max(1, maximo));
        banco.setMinimumIdle(Math.max(0, Math.min(ociosas, maximo)));
    }

    /**
     * Cada banco recebe as migrações na subida; um banco novo, vazio, sai dela com a estrutura completa. O banco que
     * ainda não existe no servidor é criado antes: incluir um cliente é só acrescentá-lo em {@code CLIENTES}.
     */
    @Bean
    public FlywayMigrationStrategy migracoesDeCadaCliente(ClientesAtendidos clientes, DataSource dataSource,
                                                          DataSourceProperties propriedades) {
        return flyway -> {
            if (!clientes.multiplos()) {
                flyway.migrate();
                return;
            }
            BancoDoCliente banco = (BancoDoCliente) dataSource;
            for (Cliente cliente : clientes.todos()) {
                criarSeNaoExistir(propriedades, cliente);
                log.info("Migrações do banco {} (cliente {})", cliente.banco(), cliente.identificador());
                Flyway.configure()
                        .configuration(flyway.getConfiguration())
                        .dataSource(banco.bancoDe(cliente))
                        .load()
                        .migrate();
            }
        };
    }

    /** Roda antes dos demais ouvintes da subida, que já podem precisar do banco de cada cliente. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ApplicationListener<ApplicationReadyEvent> subidaConcluida(DataSource dataSource) {
        return evento -> {
            if (dataSource instanceof BancoDoCliente banco) {
                banco.concluirSubida();
            }
        };
    }

    /** O e-mail enviado em segundo plano continua no cliente da requisição que o pediu. */
    @Bean
    public TaskDecorator levaClienteParaSegundoPlano() {
        return tarefa -> {
            Cliente cliente = ClienteAtual.obter();
            if (cliente == null) {
                return tarefa;
            }
            return () -> ClienteAtual.executar(cliente, tarefa);
        };
    }

    /**
     * O servidor informa se o banco existe pela tabela {@code pg_database}, consultada a partir do banco de manutenção
     * {@code postgres}, que toda instalação do PostgreSQL tem. O nome já foi validado ao ler {@code CLIENTES} (só
     * letras minúsculas, números e sublinhado), então pode ir no comando sem risco.
     */
    static void criarSeNaoExistir(DataSourceProperties propriedades, Cliente cliente) {
        String url = urlDoBanco(propriedades.determineUrl(), "postgres");
        try (Connection conexao = DriverManager.getConnection(url, propriedades.determineUsername(),
                propriedades.determinePassword())) {
            try (PreparedStatement consulta = conexao.prepareStatement("select 1 from pg_database where datname = ?")) {
                consulta.setString(1, cliente.banco());
                try (ResultSet existe = consulta.executeQuery()) {
                    if (existe.next()) {
                        return;
                    }
                }
            }
            try (Statement comando = conexao.createStatement()) {
                comando.execute("create database " + cliente.banco());
            }
            log.info("Banco {} criado para o cliente {}.", cliente.banco(), cliente.identificador());
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível conferir ou criar o banco " + cliente.banco()
                    + " do cliente " + cliente.identificador() + ": " + e.getMessage(), e);
        }
    }

    static String urlDoBanco(String urlBase, String banco) {
        Matcher partes = URL_JDBC.matcher(urlBase);
        if (!partes.matches()) {
            throw new IllegalStateException("Não foi possível montar o endereço do banco " + banco + " a partir de "
                    + "spring.datasource.url. Use o formato jdbc:postgresql://servidor:porta/banco.");
        }
        return partes.group(1) + banco + (partes.group(3) == null ? "" : partes.group(3));
    }
}
