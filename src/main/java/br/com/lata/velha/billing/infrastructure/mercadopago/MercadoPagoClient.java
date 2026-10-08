package br.com.lata.velha.billing.infrastructure.mercadopago;

import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway;
import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.billing.domain.exceptions.IntegracaoPagamentoException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Adapter para a API REST do Mercado Pago (Checkout Pro).
 * Referência: https://www.mercadopago.com.br/developers/pt/reference
 */
public class MercadoPagoClient implements MercadoPagoGateway {

    private final RestClient restClient;
    private final MercadoPagoProperties properties;

    public MercadoPagoClient(RestClient restClient, MercadoPagoProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public PreferenciaCriada criarPreferencia(Pagamento pagamento) {
        var request = new PreferenceRequest(
                List.of(new Item(
                        "OS-" + pagamento.getOrdemServicoId(),
                        pagamento.getDescricao(),
                        1,
                        pagamento.getValor(),
                        "BRL")),
                pagamento.getEmailPagador() == null ? null : new Payer(pagamento.getEmailPagador()),
                pagamento.getId().toString(),
                blankToNull(properties.notificationUrl()),
                backUrls(),
                autoReturn(),
                "LATA VELHA");

        try {
            var response = restClient.post()
                    .uri("/checkout/preferences")
                    .contentType(MediaType.APPLICATION_JSON)
                    // Repetir a criação para o mesmo pagamento não gera preferência duplicada.
                    .header("X-Idempotency-Key", pagamento.getId().toString())
                    .body(request)
                    .retrieve()
                    .body(PreferenceResponse.class);

            if (response == null || response.id() == null || response.initPoint() == null)
                throw new IntegracaoPagamentoException("Resposta inválida ao criar preferência no Mercado Pago", null);
            return new PreferenciaCriada(response.id(), response.initPoint());
        } catch (RestClientException e) {
            throw new IntegracaoPagamentoException("Falha ao criar preferência no Mercado Pago", e);
        }
    }

    @Override
    public PagamentoMercadoPago buscarPagamento(Long paymentId) {
        try {
            var response = restClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .retrieve()
                    .body(PaymentResponse.class);

            if (response == null || response.id() == null)
                throw new IntegracaoPagamentoException("Resposta inválida ao consultar payment " + paymentId, null);
            return new PagamentoMercadoPago(response.id(), response.status(), response.statusDetail(), response.externalReference());
        } catch (RestClientException e) {
            throw new IntegracaoPagamentoException("Falha ao consultar payment " + paymentId + " no Mercado Pago", e);
        }
    }

    private BackUrlsRequest backUrls() {
        var urls = properties.backUrls();
        if (urls == null || (isBlank(urls.success()) && isBlank(urls.failure()) && isBlank(urls.pending())))
            return null;
        return new BackUrlsRequest(blankToNull(urls.success()), blankToNull(urls.failure()), blankToNull(urls.pending()));
    }

    /** O Mercado Pago rejeita auto_return sem back_urls.success. */
    private String autoReturn() {
        var urls = properties.backUrls();
        return urls != null && !isBlank(urls.success()) ? "approved" : null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s;
    }

    /* ================== DTOs da API ================== */

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PreferenceRequest(
            List<Item> items,
            Payer payer,
            @JsonProperty("external_reference") String externalReference,
            @JsonProperty("notification_url") String notificationUrl,
            @JsonProperty("back_urls") BackUrlsRequest backUrls,
            @JsonProperty("auto_return") String autoReturn,
            @JsonProperty("statement_descriptor") String statementDescriptor) {
    }

    record Item(
            String id,
            String title,
            int quantity,
            @JsonProperty("unit_price") BigDecimal unitPrice,
            @JsonProperty("currency_id") String currencyId) {
    }

    record Payer(String email) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record BackUrlsRequest(String success, String failure, String pending) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PreferenceResponse(
            String id,
            @JsonProperty("init_point") String initPoint) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentResponse(
            Long id,
            String status,
            @JsonProperty("status_detail") String statusDetail,
            @JsonProperty("external_reference") String externalReference) {
    }
}
