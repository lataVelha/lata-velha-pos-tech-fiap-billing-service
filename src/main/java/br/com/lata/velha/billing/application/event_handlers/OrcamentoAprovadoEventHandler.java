package br.com.lata.velha.billing.application.event_handlers;

import br.com.lata.velha.billing.application.use_cases.AprovarOrcamentoUseCase;
import br.com.lata.velha.billing.domain.events.OrcamentoAprovadoEvent;
import br.com.lata.velha.shared.application.gateways.EventHandler;

public class OrcamentoAprovadoEventHandler implements EventHandler<OrcamentoAprovadoEvent> {

    private final AprovarOrcamentoUseCase useCase;

    public OrcamentoAprovadoEventHandler(AprovarOrcamentoUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public String eventType() {
        return "OrcamentoAprovado";
    }

    @Override
    public Class<OrcamentoAprovadoEvent> eventClass() {
        return OrcamentoAprovadoEvent.class;
    }

    @Override
    public void handle(OrcamentoAprovadoEvent event) {
        useCase.execute(event.getOsId());
    }
}
