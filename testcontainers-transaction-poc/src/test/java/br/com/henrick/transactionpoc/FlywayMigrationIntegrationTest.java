package br.com.henrick.transactionpoc;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class FlywayMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Deve aplicar as migrações do Flyway com sucesso no PostgreSQL")
    void deveExecutarMigracoesFlywayComSucesso() {
        // Valida se o Flyway executou a migração V1
        var info = flyway.info().current();

        assertThat(info).isNotNull();
        assertThat(info.getVersion().getVersion()).isEqualTo("1");
        assertThat(info.getState().isApplied()).isTrue();
    }
}