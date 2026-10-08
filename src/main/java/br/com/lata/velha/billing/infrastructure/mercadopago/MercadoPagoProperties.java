package br.com.lata.velha.billing.infrastructure.mercadopago;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "mercadopago")
public record MercadoPagoProperties(
        String baseUrl,
        String accessToken,
        String notificationUrl,
        String webhookSecret,
        Duration connectTimeout,
        Duration readTimeout,
        BackUrls backUrls) {

    public record BackUrls(String success, String failure, String pending) {
    }
}
