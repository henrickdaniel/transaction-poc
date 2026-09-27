CREATE TABLE pedidos (
                         id BIGSERIAL PRIMARY KEY,
                         cliente VARCHAR(255) NOT NULL,
                         valor NUMERIC(19, 2) NOT NULL,
                         transacao_id VARCHAR(255),
                         idempotency_key VARCHAR(255),
                         status VARCHAR(50)
);

CREATE TABLE outbox_event (
                              id BIGSERIAL PRIMARY KEY,
                              aggregate_type VARCHAR(255) NOT NULL,
                              aggregate_id VARCHAR(255) NOT NULL,
                              event_type VARCHAR(255) NOT NULL,
                              payload TEXT NOT NULL,
                              status VARCHAR(50) NOT NULL,
                              created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                              processed_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE SEQUENCE seq_outbox_event START WITH 1 INCREMENT BY 50;

CREATE TABLE audit_log (
                           id BIGSERIAL PRIMARY KEY,
                           action VARCHAR(255) NOT NULL,
                           entity_id VARCHAR(255),
                           details TEXT
);

CREATE TABLE conta (
                       id BIGSERIAL PRIMARY KEY,
                       titular VARCHAR(255),
                       saldo NUMERIC(19, 2)
);

CREATE INDEX idx_outbox_status ON outbox_event(status);
