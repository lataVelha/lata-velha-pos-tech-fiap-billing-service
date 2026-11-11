-- Story 14.2: itens do orçamento persistidos junto com a OS para que o
-- evento OrcamentoAprovado carregue execucaoServicoId/pecaId/quantidade.
CREATE TABLE orcamento_item (
    orcamento_id       UUID    NOT NULL,
    execucao_servico_id BIGINT NOT NULL,
    peca_id            BIGINT  NOT NULL,
    quantidade         INTEGER NOT NULL,
    CONSTRAINT fk_orcamento_item_orcamento
        FOREIGN KEY (orcamento_id) REFERENCES orcamento (id) ON DELETE CASCADE,
    CONSTRAINT uk_orcamento_item UNIQUE (orcamento_id, execucao_servico_id, peca_id)
);

CREATE INDEX idx_orcamento_item_orcamento ON orcamento_item (orcamento_id);
