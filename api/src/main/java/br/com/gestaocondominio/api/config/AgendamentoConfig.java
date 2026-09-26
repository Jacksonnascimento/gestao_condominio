package br.com.gestaocondominio.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga as rotinas agendadas da API, como a conclusão diária das reservas que já passaram. */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
