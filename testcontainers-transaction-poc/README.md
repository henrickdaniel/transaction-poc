## 🧪 Testes de Integração Multi-Banco

O projeto possui suporte a testes de integração para **PostgreSQL** e **Oracle Database**.

### Como funciona

* **PostgreSQL (Padrão):** Os testes convencionais utilizam as configurações padrão definidas em `src/test/resources/application.yaml`, apontando para o dialeto `org.hibernate.dialect.PostgreSQLDialect`.
* **Oracle Database:** Os testes que validam a integração com Oracle rodam via **Testcontainers** e sobrescrevem o dialeto do Hibernate dinamicamente para `org.hibernate.dialect.OracleDialect` usando `@DynamicPropertySource` (ou o profile `@ActiveProfiles("oracle")`), garantindo a compatibilidade dos metadados e DDL do Oracle.

### Requisitos para execução

Para rodar a suíte completa de testes (incluindo o Oracle):

1. Certifique-se de ter o **Docker** rodando na máquina.
2. Execute o comando Maven:

```bash
./mvnw clean test