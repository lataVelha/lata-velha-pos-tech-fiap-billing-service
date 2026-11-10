package br.com.lata.velha.billing.domain.events;

import br.com.lata.velha.shared.domain.events.AbstractDomainEvent;

import java.math.BigDecimal;
import java.util.List;

public final class OrcamentoAprovadoEvent extends AbstractDomainEvent {

    private final Long osId;
    private final BigDecimal valorTotal;
    private final List<Item> itens;

    public OrcamentoAprovadoEvent(Long osId, BigDecimal valorTotal, List<Item> itens) {
        this.osId = osId;
        this.valorTotal = valorTotal;
        this.itens = itens == null ? List.of() : List.copyOf(itens);
    }

    @Override
    public String eventType() {
        return "OrcamentoAprovado";
    }

    public Long getOsId() { return osId; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public List<Item> getItens() { return itens; }

    public record Item(Long execucaoServicoId, Long pecaId, Integer quantidade) {
    }
}
