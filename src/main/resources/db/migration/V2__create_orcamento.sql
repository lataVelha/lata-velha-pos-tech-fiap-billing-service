CREATE TABLE orcamento (
    id                UUID           PRIMARY KEY,
    ordem_servico_id  BIGINT         NOT NULL,
    valor_total       NUMERIC(12, 2) NOT NULL,
    email_cliente     VARCHAR(255),
    status            VARCHAR(20)    NOT NULL,
    criado_em         TIMESTAMP      NOT NULL,
    atualizado_em     TIMESTAMP      NOT NULL,
    CONSTRAINT uk_orcamento_ordem_servico UNIQUE (ordem_servico_id)
);
