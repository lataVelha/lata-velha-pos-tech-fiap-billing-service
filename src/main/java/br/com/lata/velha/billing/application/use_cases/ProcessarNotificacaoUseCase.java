package br.com.lata.velha.billing.application.use_cases;

import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway;
import br.com.lata.velha.billing.domain.enums.StatusPagamento;
import br.com.lata.velha.billing.domain.events.PaymentDoneEvent;
import br.com.lata.velha.billing.domain.repositories.PagamentoRepository;
import br.com.lata.velha.shared.application.gateways.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ProcessarNotificacaoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessarNotificacaoUseCase.class);

    private final PagamentoRepository repository;
    private final MercadoPagoGateway mercadoPago;
    private final EventPublisher eventPublisher;

    public ProcessarNotificacaoUseCase(PagamentoRepository repository, MercadoPagoGateway mercadoPago,
                                       EventPublisher eventPublisher) {
        this.repository = repository;
        this.mercadoPago = mercadoPago;
        this.eventPublisher = eventPublisher;
    }

    /**
     * A notificação só diz "o payment X mudou" — o status é sempre consultado na
     * API do Mercado Pago, então notificações repetidas ou fora de ordem não
     * deixam o pagamento num estado errado.
     */
    public void execute(Long paymentId) {
        var payment = mercadoPago.buscarPagamento(paymentId);

        UUID pagamentoId;
        try {
            pagamentoId = UUID.fromString(payment.referenciaExterna());
        } catch (IllegalArgumentException | NullPointerException e) {
            logger.warn("Payment sem external_reference reconhecida, ignorando - paymentId={}, externalReference={}",
                    paymentId, payment.referenciaExterna());
            return;
        }

        var encontrado = repository.buscarPorId(pagamentoId);
        if (encontrado.isEmpty()) {
            logger.warn("Pagamento da notificação não encontrado, ignorando - paymentId={}, pagamentoId={}", paymentId, pagamentoId);
            return;
        }

        var pagamento = encontrado.get();
        var anterior = pagamento.getStatus();
        var novoStatus = StatusPagamento.fromMercadoPago(payment.status());
        if (pagamento.atualizarStatus(novoStatus, payment.id(), payment.statusDetalhe())) {
            repository.salvar(pagamento);
            logger.info("Status do pagamento atualizado - pagamentoId={}, paymentId={}, {} -> {}",
                    pagamentoId, paymentId, anterior, pagamento.getStatus());
            if (pagamento.isAprovado()) {
                eventPublisher.publish(new PaymentDoneEvent(pagamento.getOrdemServicoId()));
                logger.info("PaymentDone publicado - osId={}, pagamentoId={}", pagamento.getOrdemServicoId(), pagamentoId);
            }
        } else {
            logger.info("Notificação sem mudança de status - pagamentoId={}, paymentId={}, statusAtual={}, statusMercadoPago={}",
                    pagamentoId, paymentId, anterior, novoStatus);
        }
    }
}
