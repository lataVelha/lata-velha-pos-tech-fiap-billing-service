package br.com.lata.velha.billing.domain.events;

import br.com.lata.velha.shared.domain.events.AbstractDomainEvent;

import java.math.BigDecimal;

public final class OrcamentoAprovadoEvent extends AbstractDomainEvent {

    private final Long osId;
    private final BigDecimal valorTotal;

    public OrcamentoAprovadoEvent(Long osId, BigDecimal valorTotal) {
        this.osId = osId;
        this.valorTotal = valorTotal;
    }

    @Override
    public String eventType() {
        return "OrcamentoAprovado";
    }

    public Long getOsId() { return osId; }
    public BigDecimal getValorTotal() { return valorTotal; }
}
