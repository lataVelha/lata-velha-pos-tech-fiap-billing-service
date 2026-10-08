package br.com.lata.velha.billing.api.dtos;

import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.billing.domain.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagamentoResponse(
        UUID id,
        Long ordemServicoId,
        BigDecimal valor,
        String descricao,
        StatusPagamento status,
        String statusDetalhe,
        String checkoutUrl,
        Long mercadoPagoPaymentId,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static PagamentoResponse from(Pagamento p) {
        return new PagamentoResponse(p.getId(), p.getOrdemServicoId(), p.getValor(), p.getDescricao(), p.getStatus(),
                p.getStatusDetalhe(), p.getCheckoutUrl(), p.getMercadoPagoPaymentId(), p.getCriadoEm(), p.getAtualizadoEm());
    }
}
