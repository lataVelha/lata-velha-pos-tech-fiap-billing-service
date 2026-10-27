package br.com.lata.velha.billing.infrastructure.config;

import br.com.lata.velha.billing.application.event_handlers.DiagnosticoFinalizadoEventHandler;
import br.com.lata.velha.billing.application.event_handlers.OSEndedEventHandler;
import br.com.lata.velha.billing.application.use_cases.AprovarOrcamentoUseCase;
import br.com.lata.velha.billing.application.use_cases.CriarPagamentoUseCase;
import br.com.lata.velha.billing.application.use_cases.EmitirCobrancaUseCase;
import br.com.lata.velha.billing.application.use_cases.ReceberDiagnosticoFinalizadoUseCase;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import br.com.lata.velha.billing.infrastructure.inventory.InventoryServiceClient;
import br.com.lata.velha.shared.application.gateways.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class BillingEventosBeansConfig {

    @Value("${inventory-service.base-url:http://localhost:8082}")
    private String inventoryBaseUrl;

    @Value("${billing.mail.from:noreply@latavelha.com.br}")
    private String emailRemetente;

    @Bean
    public InventoryServiceClient inventoryServiceClient(RestClient.Builder builder) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(10));
        var client = builder
                .baseUrl(inventoryBaseUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
        return new InventoryServiceClient(client);
    }

    @Bean
    public ReceberDiagnosticoFinalizadoUseCase receberDiagnosticoFinalizadoUseCase(
            OrcamentoRepository orcamentoRepository,
            InventoryServiceClient inventoryServiceClient,
            JavaMailSender mailSender) {
        return new ReceberDiagnosticoFinalizadoUseCase(orcamentoRepository, inventoryServiceClient, mailSender, emailRemetente);
    }

    @Bean
    public AprovarOrcamentoUseCase aprovarOrcamentoUseCase(OrcamentoRepository orcamentoRepository,
                                                            EventPublisher eventPublisher) {
        return new AprovarOrcamentoUseCase(orcamentoRepository, eventPublisher);
    }

    @Bean
    public EmitirCobrancaUseCase emitirCobrancaUseCase(OrcamentoRepository orcamentoRepository,
                                                        CriarPagamentoUseCase criarPagamentoUseCase,
                                                        EventPublisher eventPublisher) {
        return new EmitirCobrancaUseCase(orcamentoRepository, criarPagamentoUseCase, eventPublisher);
    }

    @Bean
    public DiagnosticoFinalizadoEventHandler diagnosticoFinalizadoEventHandler(
            ReceberDiagnosticoFinalizadoUseCase useCase) {
        return new DiagnosticoFinalizadoEventHandler(useCase);
    }

    @Bean
    public OSEndedEventHandler osEndedEventHandler(EmitirCobrancaUseCase useCase) {
        return new OSEndedEventHandler(useCase);
    }
}
