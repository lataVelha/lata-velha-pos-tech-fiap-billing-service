package br.com.lata.velha.billing.infrastructure.config;

import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway;
import br.com.lata.velha.billing.application.use_cases.BuscarPagamentoUseCase;
import br.com.lata.velha.billing.application.use_cases.CriarPagamentoUseCase;
import br.com.lata.velha.billing.application.use_cases.ProcessarNotificacaoUseCase;
import br.com.lata.velha.billing.domain.repositories.PagamentoRepository;
import br.com.lata.velha.billing.infrastructure.mercadopago.MercadoPagoClient;
import br.com.lata.velha.billing.infrastructure.mercadopago.MercadoPagoProperties;
import br.com.lata.velha.billing.infrastructure.mercadopago.WebhookSignatureValidator;
import br.com.lata.velha.shared.application.gateways.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class PagamentoBeansConfig {

    private static final Logger logger = LoggerFactory.getLogger(PagamentoBeansConfig.class);

    @Bean
    public RestClient mercadoPagoRestClient(RestClient.Builder builder, MercadoPagoProperties properties) {
        if (properties.accessToken() == null || properties.accessToken().isBlank())
            logger.warn("MERCADOPAGO_ACCESS_TOKEN não configurado — chamadas ao Mercado Pago vão falhar");

        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                .build();
    }

    @Bean
    public MercadoPagoGateway mercadoPagoGateway(RestClient mercadoPagoRestClient, MercadoPagoProperties properties) {
        return new MercadoPagoClient(mercadoPagoRestClient, properties);
    }

    @Bean
    public WebhookSignatureValidator webhookSignatureValidator(MercadoPagoProperties properties) {
        var validator = new WebhookSignatureValidator(properties.webhookSecret());
        if (!validator.habilitado())
            logger.warn("MERCADOPAGO_WEBHOOK_SECRET não configurado — assinatura dos webhooks NÃO será validada");
        return validator;
    }

    @Bean
    public CriarPagamentoUseCase criarPagamentoUseCase(PagamentoRepository repository, MercadoPagoGateway mercadoPago) {
        return new CriarPagamentoUseCase(repository, mercadoPago);
    }

    @Bean
    public BuscarPagamentoUseCase buscarPagamentoUseCase(PagamentoRepository repository) {
        return new BuscarPagamentoUseCase(repository);
    }

    @Bean
    public ProcessarNotificacaoUseCase processarNotificacaoUseCase(PagamentoRepository repository,
                                                                    MercadoPagoGateway mercadoPago,
                                                                    EventPublisher eventPublisher) {
        return new ProcessarNotificacaoUseCase(repository, mercadoPago, eventPublisher);
    }
}
