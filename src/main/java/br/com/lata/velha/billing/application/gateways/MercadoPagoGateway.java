package br.com.lata.velha.billing.application.gateways;

import br.com.lata.velha.billing.domain.entities.Pagamento;

public interface MercadoPagoGateway {

    /** Cria uma preferência do Checkout Pro para o pagamento. */
    PreferenciaCriada criarPreferencia(Pagamento pagamento);

    /** Consulta o estado atual de um payment no Mercado Pago. */
    PagamentoMercadoPago buscarPagamento(Long paymentId);

    record PreferenciaCriada(String id, String checkoutUrl) {
    }

    record PagamentoMercadoPago(Long id, String status, String statusDetalhe, String referenciaExterna) {
    }
}
