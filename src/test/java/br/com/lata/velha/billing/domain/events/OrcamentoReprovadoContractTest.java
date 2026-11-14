package br.com.lata.velha.billing.domain.events;

import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato de OrcamentoReprovado.
 *
 * O JSON abaixo é o fixture esperado pelo os-service ao ouvir o evento —
 * qualquer mudança de nome/tipo quebra os dois lados intencionalmente.
 */
class OrcamentoReprovadoContractTest {

    private final EventSerializer serializer = new EventSerializer();

    @Test
    @DisplayName("OrcamentoReprovado publica osId compatível com o os-service")
    void devePublicarPayloadCompativel() {
        var event = new OrcamentoReprovadoEvent(42L);

        String json = serializer.serialize(event);

        assertThat(json)
                .contains("\"eventType\":\"OrcamentoReprovado\"")
                .contains("\"osId\":42");

        var envelope = serializer.deserializeEnvelope(json);
        var roundtrip = serializer.deserializePayload(envelope, OrcamentoReprovadoEvent.class);
        assertThat(roundtrip.getOsId()).isEqualTo(42L);
    }
}
