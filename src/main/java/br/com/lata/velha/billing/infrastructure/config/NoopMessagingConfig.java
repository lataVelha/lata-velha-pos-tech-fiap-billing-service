package br.com.lata.velha.billing.infrastructure.config;

import br.com.lata.velha.shared.application.gateways.EventPublisher;
import br.com.lata.velha.shared.domain.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Beans de fallback para testes e ambientes sem RabbitMQ/SMTP configurados.
 * Ativado com messaging.provider=noop.
 */
@Configuration
@ConditionalOnProperty(name = "messaging.provider", havingValue = "noop")
public class NoopMessagingConfig {

    private static final Logger logger = LoggerFactory.getLogger(NoopMessagingConfig.class);

    @Bean
    public EventPublisher noopEventPublisher() {
        return event -> logger.debug("NoopEventPublisher: evento descartado - type={}", event.eventType());
    }

    @Bean
    public JavaMailSender noopMailSender() {
        return new JavaMailSenderImpl();
    }
}
