package br.com.lata.velha.billing.api.controllers;

import br.com.lata.velha.billing.api.dtos.NotificacaoMercadoPagoRequest;
import br.com.lata.velha.billing.application.use_cases.ProcessarNotificacaoUseCase;
import br.com.lata.velha.billing.infrastructure.mercadopago.WebhookSignatureValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Recebe as notificações (Webhooks) do Mercado Pago. Respondemos 200 para tudo
 * que não interessa (outros tópicos) para o Mercado Pago não ficar reenviando;
 * data.id inválido responde 400 (erro permanente, também sem retry). Falhas
 * transitórias (ex.: API do Mercado Pago fora) sobem como 5xx pelo
 * GlobalExceptionHandler e o Mercado Pago tenta de novo.
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
            @RequestBody(required = false) NotificacaoMercadoPagoRequest body,
            HttpServletRequest request) {

        var dataId = dataIdParam != null ? dataIdParam
                : body != null && body.data() != null ? body.data().id() : null;
        var type = typeParam != null ? typeParam : body != null ? body.type() : null;

        if (!signatureValidator.valido(xSignature, xRequestId, dataId)) {
            // Não logamos os valores crus (vêm do cliente e poderiam injetar
            // CR/LF no log). A presença já permite diagnosticar; para conferir
            // a assinatura, use o request-id no painel do Mercado Pago.
            logger.warn("Webhook com assinatura inválida - dataIdPresente={}, requestIdPresente={}, "
                            + "signaturePresente={}, queryPresente={}",
                    dataId != null, xRequestId != null, xSignature != null, request.getQueryString() != null);
            return ResponseEntity.status(401).build();
        }

        if (!"payment".equals(type)) {
            logger.info("Webhook ignorado (tópico não tratado) - typePresente={}, dataIdPresente={}",
                    type != null, dataId != null);
            return ResponseEntity.ok().build();
        }

        Long paymentId;
        try {
            paymentId = Long.valueOf(dataId);
        } catch (NumberFormatException e) {
            logger.warn("Webhook com data.id inválido - dataIdPresente={}", dataId != null);
            return ResponseEntity.badRequest().build();
        }

        processarNotificacao.execute(paymentId);
        return ResponseEntity.ok().build();
    }
}
