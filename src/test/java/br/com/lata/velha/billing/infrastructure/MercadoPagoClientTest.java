package br.com.lata.velha.billing.infrastructure;

import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.billing.domain.exceptions.IntegracaoPagamentoException;
import br.com.lata.velha.billing.infrastructure.mercadopago.MercadoPagoClient;
import br.com.lata.velha.billing.infrastructure.mercadopago.MercadoPagoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoClientTest {

    private MockRestServiceServer server;
    private MercadoPagoClient client;

    @BeforeEach
    void setUp() {
        var properties = new MercadoPagoProperties("https://api.mp.test", "TEST-token",
                "https://api.lata-velha.test/pagamentos/webhook", "s", Duration.ofSeconds(1), Duration.ofSeconds(1),
                new MercadoPagoProperties.BackUrls("https://lata-velha.test/ok", "", null));
        var builder = RestClient.builder().baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.accessToken());
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MercadoPagoClient(builder.build(), properties);
    }

    @Test
    void criaPreferencia() {
        var pagamento = Pagamento.create(42L, new BigDecimal("199.90"), "Revisão completa", "cliente@teste.com");

        server.expect(requestTo("https://api.mp.test/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer TEST-token"))
                .andExpect(header("X-Idempotency-Key", pagamento.getId().toString()))
                .andExpect(jsonPath("$.external_reference").value(pagamento.getId().toString()))
                .andExpect(jsonPath("$.items[0].id").value("OS-42"))
                .andExpect(jsonPath("$.items[0].unit_price").value(199.90))
                .andExpect(jsonPath("$.items[0].currency_id").value("BRL"))
                .andExpect(jsonPath("$.payer.email").value("cliente@teste.com"))
                .andExpect(jsonPath("$.notification_url").value("https://api.lata-velha.test/pagamentos/webhook"))
                .andExpect(jsonPath("$.back_urls.success").value("https://lata-velha.test/ok"))
                .andExpect(jsonPath("$.back_urls.failure").doesNotExist())
                .andExpect(jsonPath("$.auto_return").value("approved"))
                .andRespond(withSuccess("""
                        {"id":"123-abc","init_point":"https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=123-abc","collector_id":1}
                        """, MediaType.APPLICATION_JSON));

        var preferencia = client.criarPreferencia(pagamento);

        assertThat(preferencia.id()).isEqualTo("123-abc");
        assertThat(preferencia.checkoutUrl()).contains("pref_id=123-abc");
        server.verify();
    }

    @Test
    void erroAoCriarPreferenciaViraIntegracaoException() {
        var pagamento = Pagamento.create(42L, BigDecimal.TEN, "x", null);
        server.expect(requestTo("https://api.mp.test/checkout/preferences"))
                .andExpect(jsonPath("$.payer").doesNotExist())
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.criarPreferencia(pagamento)).isInstanceOf(IntegracaoPagamentoException.class);
    }

    @Test
    void buscaPagamento() {
        server.expect(requestTo("https://api.mp.test/v1/payments/987"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":987,"status":"approved","status_detail":"accredited","external_reference":"ref","transaction_amount":10}
                        """, MediaType.APPLICATION_JSON));

        var payment = client.buscarPagamento(987L);

        assertThat(payment.id()).isEqualTo(987L);
        assertThat(payment.status()).isEqualTo("approved");
        assertThat(payment.statusDetalhe()).isEqualTo("accredited");
        assertThat(payment.referenciaExterna()).isEqualTo("ref");
    }

    @Test
    void erroAoBuscarPagamentoViraIntegracaoException() {
        server.expect(requestTo("https://api.mp.test/v1/payments/1")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> client.buscarPagamento(1L)).isInstanceOf(IntegracaoPagamentoException.class);
    }
}
