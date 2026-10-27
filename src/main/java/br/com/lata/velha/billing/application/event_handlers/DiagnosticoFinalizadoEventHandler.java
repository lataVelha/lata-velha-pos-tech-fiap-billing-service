package br.com.lata.velha.billing.application.event_handlers;

import br.com.lata.velha.billing.application.use_cases.ReceberDiagnosticoFinalizadoUseCase;
import br.com.lata.velha.billing.domain.events.DiagnosticoFinalizadoEvent;
import br.com.lata.velha.shared.application.gateways.EventHandler;

public class DiagnosticoFinalizadoEventHandler implements EventHandler<DiagnosticoFinalizadoEvent> {

    private final ReceberDiagnosticoFinalizadoUseCase useCase;

    public DiagnosticoFinalizadoEventHandler(ReceberDiagnosticoFinalizadoUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public String eventType() {
        return "DiagnosticoFinalizado";
    }

    @Override
    public Class<DiagnosticoFinalizadoEvent> eventClass() {
        return DiagnosticoFinalizadoEvent.class;
    }

    @Override
    public void handle(DiagnosticoFinalizadoEvent event) {
        useCase.execute(event);
    }
}
