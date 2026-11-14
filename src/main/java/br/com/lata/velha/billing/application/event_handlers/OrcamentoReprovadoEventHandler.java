package br.com.lata.velha.billing.application.event_handlers;

import br.com.lata.velha.billing.application.use_cases.ReprovarOrcamentoUseCase;
import br.com.lata.velha.billing.domain.events.OrcamentoReprovadoEvent;
import br.com.lata.velha.shared.application.gateways.EventHandler;

public class OrcamentoReprovadoEventHandler implements EventHandler<OrcamentoReprovadoEvent> {

    private final ReprovarOrcamentoUseCase useCase;

    public OrcamentoReprovadoEventHandler(ReprovarOrcamentoUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public String eventType() {
        return "OrcamentoReprovado";
    }

    @Override
    public Class<OrcamentoReprovadoEvent> eventClass() {
        return OrcamentoReprovadoEvent.class;
    }

    @Override
    public void handle(OrcamentoReprovadoEvent event) {
        useCase.execute(event.getOsId());
    }
}
