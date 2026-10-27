package br.com.lata.velha.billing.infrastructure.inventory;

import br.com.lata.velha.billing.application.gateways.InventoryGateway;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

public class InventoryServiceClient implements InventoryGateway {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceClient.class);

    private final RestClient restClient;

    public InventoryServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<BigDecimal> buscarValorPeca(Long pecaId) {
        try {
            var response = restClient.get()
                    .uri("/pecas/{id}", pecaId)
                    .retrieve()
                    .body(PecaResponse.class);
            if (response == null || response.valor() == null) {
                logger.warn("Peça não encontrada no inventory-service - pecaId={}", pecaId);
                return Optional.empty();
            }
            return Optional.of(response.valor());
        } catch (RestClientException e) {
            logger.warn("Falha ao buscar preço da peça no inventory-service - pecaId={}", pecaId, e);
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PecaResponse(Long id, String nome, BigDecimal valor) {}
}
