CREATE TABLE pagamento (
    id                       UUID           PRIMARY KEY,
    ordem_servico_id         BIGINT         NOT NULL,
    valor                    NUMERIC(12, 2) NOT NULL,
    descricao                VARCHAR(255)   NOT NULL,
    email_pagador            VARCHAR(255),
    status                   VARCHAR(30)    NOT NULL,
    preferencia_id           VARCHAR(100),
    checkout_url             VARCHAR(500),
    mercado_pago_payment_id  BIGINT,
    status_detalhe           VARCHAR(100),
    criado_em                TIMESTAMP      NOT NULL,
    atualizado_em            TIMESTAMP      NOT NULL,
    CONSTRAINT uk_pagamento_ordem_servico UNIQUE (ordem_servico_id)
);
