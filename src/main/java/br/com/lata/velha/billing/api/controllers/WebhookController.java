package br.com.lata.velha.billing.api.controllers;

import br.com.lata.velha.billing.api.dtos.NotificacaoMercadoPagoRequest;
import br.com.lata.velha.billing.application.use_cases.ProcessarNotificacaoUseCase;
import br.com.lata.velha.billing.infrastructure.mercadopago.WebhookSignatureValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Recebe as notificações (Webhooks) do Mercado Pago. Respondemos 200 para tudo
 * que não interessa (outros tópicos, ids inválidos) para o Mercado Pago não
 * ficar reenviando; falhas transitórias (ex.: API do Mercado Pago fora) sobem
 * como 5xx pelo GlobalExceptionHandler e o Mercado Pago tenta de novo.
 */
@RestController
@RequestMapping("/pagamentos/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhook Mercado Pago")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final ProcessarNotificacaoUseCase processarNotificacao;
    private final WebhookSignatureValidator signatureValidator;

    @PostMapping
    @Operation(summary = "Endpoint de notificação chamado pelo Mercado Pago")
    public ResponseEntity<Void> receber(
            @RequestParam(name = "data.id", required = false) String dataIdParam,
            @RequestParam(name = "type", required = false) String typeParam,
            @RequestHeader(name = "x-signature", required = false) String xSignature,
            @RequestHeader(name = "x-request-id", required = false) String xRequestId,
            @RequestBody(required = false) NotificacaoMercadoPagoRequest body) {

        var dataId = dataIdParam != null ? dataIdParam
                : body != null && body.data() != null ? body.data().id() : null;
        var type = typeParam != null ? typeParam : body != null ? body.type() : null;

        if (!signatureValidator.valido(xSignature, xRequestId, dataId)) {
            logger.warn("Webhook com assinatura inválida - dataId={}, requestId={}", dataId, xRequestId);
            return ResponseEntity.status(401).build();
        }

        if (!"payment".equals(type)) {
            logger.info("Webhook ignorado (tópico não tratado) - type={}, dataId={}", type, dataId);
            return ResponseEntity.ok().build();
        }

        Long paymentId;
        try {
            paymentId = Long.valueOf(dataId);
        } catch (NumberFormatException e) {
            logger.warn("Webhook com data.id inválido, ignorando - dataId={}", dataId);
            return ResponseEntity.ok().build();
        }

        processarNotificacao.execute(paymentId);
        return ResponseEntity.ok().build();
    }
}
