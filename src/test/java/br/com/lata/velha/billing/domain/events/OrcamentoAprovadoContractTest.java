package br.com.lata.velha.billing.domain.events;

import br.com.lata.velha.shared.infrastructure.messaging.EventSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato de OrcamentoAprovado (Story 14.2 / 15.1).
 *
 * O JSON abaixo é o MESMO fixture usado pelo inventory-service em
 * OrcamentoAprovadoContractTest — qualquer mudança de nome/tipo quebra os dois
 * lados intencionalmente.
 */
class OrcamentoAprovadoContractTest {

    private static final String EXPECTED_ITENS =
            "\"itens\":[{\"execucaoServicoId\":10,\"pecaId\":100,\"quantidade\":2}]";

    private final EventSerializer serializer = new EventSerializer();

    @Test
    @DisplayName("OrcamentoAprovado publica osId, valorTotal e itens[] compatíveis com o inventory")
    void devePublicarPayloadCompativel() {
        var event = new OrcamentoAprovadoEvent(42L, new BigDecimal("250.00"),
                List.of(new OrcamentoAprovadoEvent.Item(10L, 100L, 2)));

        String json = serializer.serialize(event);

        assertThat(json)
                .contains("\"eventType\":\"OrcamentoAprovado\"")
                .contains("\"osId\":42")
                .contains("\"valorTotal\":")
                .contains(EXPECTED_ITENS);

        var envelope = serializer.deserializeEnvelope(json);
        var roundtrip = serializer.deserializePayload(envelope, OrcamentoAprovadoEvent.class);
        assertThat(roundtrip.getValorTotal()).isEqualByComparingTo("250.00");
        assertThat(roundtrip.getItens()).hasSize(1);
    }

    @Test
    @DisplayName("payload sem itens não quebra o contrato")
    void deveAceitarOrcamentoSemItens() {
        var event = new OrcamentoAprovadoEvent(42L, BigDecimal.TEN, List.of());

        String json = serializer.serialize(event);

        assertThat(json).contains("\"itens\":[]");
    }
}
