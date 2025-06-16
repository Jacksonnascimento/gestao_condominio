package br.com.gestaocondominio.api;

import br.com.gestaocondominio.api.security.properties.JwtProperties; 
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties; 

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class) 
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

}