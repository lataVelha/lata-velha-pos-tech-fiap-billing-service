package br.com.lata.velha.billing.domain.events;

import br.com.lata.velha.shared.domain.events.AbstractDomainEvent;

public final class OSEndedEvent extends AbstractDomainEvent {

    private final Long osId;

    public OSEndedEvent(Long osId) {
        this.osId = osId;
    }

    @Override
    public String eventType() {
        return "OSEnded";
    }

    public Long getOsId() { return osId; }
}
