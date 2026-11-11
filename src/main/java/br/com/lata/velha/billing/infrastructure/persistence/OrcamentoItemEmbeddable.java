package br.com.lata.velha.billing.infrastructure.persistence;

import br.com.lata.velha.billing.domain.entities.OrcamentoItem;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class OrcamentoItemEmbeddable {

    @Column(name = "execucao_servico_id", nullable = false)
    private Long execucaoServicoId;

    @Column(name = "peca_id", nullable = false)
    private Long pecaId;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    protected OrcamentoItemEmbeddable() {
    }

    private OrcamentoItemEmbeddable(Long execucaoServicoId, Long pecaId, Integer quantidade) {
        this.execucaoServicoId = execucaoServicoId;
        this.pecaId = pecaId;
        this.quantidade = quantidade;
    }

    public static OrcamentoItemEmbeddable from(OrcamentoItem item) {
        return new OrcamentoItemEmbeddable(item.execucaoServicoId(), item.pecaId(), item.quantidade());
    }

    public OrcamentoItem toDomain() {
        return new OrcamentoItem(execucaoServicoId, pecaId, quantidade);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrcamentoItemEmbeddable other)) return false;
        return Objects.equals(execucaoServicoId, other.execucaoServicoId)
                && Objects.equals(pecaId, other.pecaId)
                && Objects.equals(quantidade, other.quantidade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(execucaoServicoId, pecaId, quantidade);
    }
}
