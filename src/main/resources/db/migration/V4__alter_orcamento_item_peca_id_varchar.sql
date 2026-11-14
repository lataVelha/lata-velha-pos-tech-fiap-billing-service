ALTER TABLE orcamento_item
    ALTER COLUMN peca_id TYPE VARCHAR(64) USING peca_id::VARCHAR;
