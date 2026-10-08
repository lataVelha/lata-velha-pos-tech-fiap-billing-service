package br.com.lata.velha.billing.infrastructure.mercadopago;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Valida o header x-signature das notificações do Mercado Pago.
 *
 * O header tem o formato "ts=<timestamp>,v1=<hmac>" e o hmac é o
 * HMAC-SHA256 (hex), com a chave secreta do painel "Webhooks", do manifesto
 * "id:<data.id>;request-id:<x-request-id>;ts:<ts>;" — onde data.id é o
 * query param da URL (em minúsculas, se alfanumérico).
 * Ver https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks
 */
public class WebhookSignatureValidator {

    private final String secret;

    public WebhookSignatureValidator(String secret) {
        this.secret = secret;
    }

    public boolean habilitado() {
        return secret != null && !secret.isBlank();
    }

    public boolean valido(String xSignature, String xRequestId, String dataId) {
        if (!habilitado())
            return true;
        if (xSignature == null || xSignature.isBlank())
            return false;

        String ts = null;
        String v1 = null;
        for (var parte : xSignature.split(",")) {
            var kv = parte.split("=", 2);
            if (kv.length != 2)
                continue;
            switch (kv[0].trim()) {
                case "ts" -> ts = kv[1].trim();
                case "v1" -> v1 = kv[1].trim();
                default -> { /* ignora chaves desconhecidas */ }
            }
        }
        if (ts == null || v1 == null)
            return false;

        var manifesto = new StringBuilder();
        if (dataId != null && !dataId.isBlank())
            manifesto.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
        if (xRequestId != null && !xRequestId.isBlank())
            manifesto.append("request-id:").append(xRequestId).append(';');
        manifesto.append("ts:").append(ts).append(';');

        var esperado = hmacSha256Hex(manifesto.toString());
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256Hex(String data) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponível", e);
        }
    }
}
