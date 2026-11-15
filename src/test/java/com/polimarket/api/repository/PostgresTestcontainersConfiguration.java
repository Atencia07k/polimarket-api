package com.polimarket.api.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL efimero para las pruebas de repositorios. Usa la misma version que compose.yaml
 * y nunca toca la base de desarrollo polimarket_db. Spring cachea el contexto, por lo que
 * todas las clases de prueba comparten un unico contenedor por ejecucion.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18.7"));
    }
}
