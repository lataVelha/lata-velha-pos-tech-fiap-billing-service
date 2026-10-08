package br.com.lata.velha.billing.infrastructure.persistence;

import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.billing.domain.enums.StatusPagamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
@NoArgsConstructor
public class PagamentoEntity {

    @Id
    private UUID id;

    @Column(name = "ordem_servico_id", nullable = false, unique = true)
    private Long ordemServicoId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "email_pagador")
    private String emailPagador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPagamento status;

    @Column(name = "preferencia_id", length = 100)
    private String preferenciaId;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    @Column(name = "mercado_pago_payment_id")
    private Long mercadoPagoPaymentId;

    @Column(name = "status_detalhe", length = 100)
    private String statusDetalhe;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public static PagamentoEntity fromDomain(Pagamento p) {
        var e = new PagamentoEntity();
        e.id = p.getId();
        e.ordemServicoId = p.getOrdemServicoId();
        e.valor = p.getValor();
        e.descricao = p.getDescricao();
        e.emailPagador = p.getEmailPagador();
        e.status = p.getStatus();
        e.preferenciaId = p.getPreferenciaId();
        e.checkoutUrl = p.getCheckoutUrl();
        e.mercadoPagoPaymentId = p.getMercadoPagoPaymentId();
        e.statusDetalhe = p.getStatusDetalhe();
        e.criadoEm = p.getCriadoEm();
        e.atualizadoEm = p.getAtualizadoEm();
        return e;
    }

    public Pagamento toDomain() {
        return new Pagamento(id, ordemServicoId, valor, descricao, emailPagador, status, preferenciaId,
                checkoutUrl, mercadoPagoPaymentId, statusDetalhe, criadoEm, atualizadoEm);
    }
}
