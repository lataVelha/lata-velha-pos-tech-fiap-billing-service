package br.com.lata.velha.billing.domain.events;

import br.com.lata.velha.billing.domain.events.DiagnosticoFinalizadoEvent.ItemDiagnostico;
import br.com.lata.velha.billing.domain.events.DiagnosticoFinalizadoEvent.ItemPeca;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventsTest {

    @Test
    void diagnosticoFinalizadoEventExpoeDados() {
        var peca = new ItemPeca(3L, 2);
        var item = new ItemDiagnostico(1L, 2L, new BigDecimal("10.00"), List.of(peca));
        var event = new DiagnosticoFinalizadoEvent(5L, List.of(item));

        assertThat(event.eventType()).isEqualTo("DiagnosticoFinalizado");
        assertThat(event.getOsId()).isEqualTo(5L);
        assertThat(event.getItens()).containsExactly(item);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();

        assertThat(item.execucaoServicoId()).isEqualTo(1L);
        assertThat(item.servicoId()).isEqualTo(2L);
        assertThat(item.valorMaoDeObra()).isEqualByComparingTo("10.00");
        assertThat(item.pecas()).containsExactly(peca);
        assertThat(peca.pecaId()).isEqualTo(3L);
        assertThat(peca.quantidade()).isEqualTo(2);
    }

    @Test
    void osEndedEventExpoeDados() {
        var event = new OSEndedEvent(6L);

        assertThat(event.eventType()).isEqualTo("OSEnded");
        assertThat(event.getOsId()).isEqualTo(6L);
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    void cobrancaEmitidaEventExpoeDados() {
        var event = new CobrancaEmitidaEvent(7L);

        assertThat(event.eventType()).isEqualTo("CobrancaEmitida");
        assertThat(event.getOsId()).isEqualTo(7L);
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    void paymentDoneEventExpoeDados() {
        var event = new PaymentDoneEvent(8L);

        assertThat(event.eventType()).isEqualTo("PaymentDone");
        assertThat(event.getOsId()).isEqualTo(8L);
        assertThat(event.eventId()).isNotNull();
    }
}
