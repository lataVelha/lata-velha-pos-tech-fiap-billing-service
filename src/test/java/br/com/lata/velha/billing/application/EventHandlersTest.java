package br.com.lata.velha.billing.application;

import br.com.lata.velha.billing.application.event_handlers.DiagnosticoFinalizadoEventHandler;
import br.com.lata.velha.billing.application.event_handlers.OSEndedEventHandler;
import br.com.lata.velha.billing.application.use_cases.EmitirCobrancaUseCase;
import br.com.lata.velha.billing.application.use_cases.ReceberDiagnosticoFinalizadoUseCase;
import br.com.lata.velha.billing.domain.events.DiagnosticoFinalizadoEvent;
import br.com.lata.velha.billing.domain.events.OSEndedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventHandlersTest {

    @Test
    void diagnosticoFinalizadoHandlerDelegaParaUseCase() {
        var useCase = mock(ReceberDiagnosticoFinalizadoUseCase.class);
        var handler = new DiagnosticoFinalizadoEventHandler(useCase);
        var event = new DiagnosticoFinalizadoEvent(7L, List.of());

        assertThat(handler.eventType()).isEqualTo("DiagnosticoFinalizado");
        assertThat(handler.eventClass()).isEqualTo(DiagnosticoFinalizadoEvent.class);

        handler.handle(event);

        verify(useCase).execute(event);
    }

    @Test
    void osEndedHandlerDelegaParaUseCase() {
        var useCase = mock(EmitirCobrancaUseCase.class);
        var handler = new OSEndedEventHandler(useCase);
        var event = new OSEndedEvent(8L);

        assertThat(handler.eventType()).isEqualTo("OSEnded");
        assertThat(handler.eventClass()).isEqualTo(OSEndedEvent.class);

        handler.handle(event);

        verify(useCase).execute(8L);
    }
}
