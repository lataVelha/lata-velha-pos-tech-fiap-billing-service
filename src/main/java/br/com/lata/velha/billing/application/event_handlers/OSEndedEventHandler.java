package br.com.lata.velha.billing.application.event_handlers;

import br.com.lata.velha.billing.application.use_cases.EmitirCobrancaUseCase;
import br.com.lata.velha.billing.domain.events.OSEndedEvent;
import br.com.lata.velha.shared.application.gateways.EventHandler;

public class OSEndedEventHandler implements EventHandler<OSEndedEvent> {

    private final EmitirCobrancaUseCase useCase;

    public OSEndedEventHandler(EmitirCobrancaUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public String eventType() {
        return "OSEnded";
    }

    @Override
    public Class<OSEndedEvent> eventClass() {
        return OSEndedEvent.class;
    }

    @Override
    public void handle(OSEndedEvent event) {
        useCase.execute(event.getOsId());
    }
}
