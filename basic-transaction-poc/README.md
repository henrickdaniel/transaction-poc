### **Como executar a POC localmente**

Dentro de `basic-transaction-poc/`:

1. **Subir o banco de dados:**
```bash
docker compose up -d

```


2. **Rodar a compilação e os testes:**
```bash
mvn clean test

```


3. **Derrubar o container ao finalizar (opcional):**
```bash
docker compose down

```