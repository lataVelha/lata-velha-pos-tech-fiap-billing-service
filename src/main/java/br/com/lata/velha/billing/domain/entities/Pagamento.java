package br.com.lata.velha.billing.domain.entities;

import br.com.lata.velha.billing.domain.enums.StatusPagamento;
import br.com.lata.velha.billing.domain.exceptions.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public final class Pagamento {
    private final UUID id;
    private final Long ordemServicoId;
    private final BigDecimal valor;
    private final String descricao;
    private final String emailPagador;
    private StatusPagamento status;
    private String preferenciaId;
    private String checkoutUrl;
    private Long mercadoPagoPaymentId;
    private String statusDetalhe;
    private final LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Pagamento(UUID id, Long ordemServicoId, BigDecimal valor, String descricao, String emailPagador,
                     StatusPagamento status, String preferenciaId, String checkoutUrl, Long mercadoPagoPaymentId,
                     String statusDetalhe, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.ordemServicoId = ordemServicoId;
        this.valor = valor;
        this.descricao = descricao;
        this.emailPagador = emailPagador;
        this.status = status;
        this.preferenciaId = preferenciaId;
        this.checkoutUrl = checkoutUrl;
        this.mercadoPagoPaymentId = mercadoPagoPaymentId;
        this.statusDetalhe = statusDetalhe;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public static Pagamento create(Long ordemServicoId, BigDecimal valor, String descricao, String emailPagador) {
        if (ordemServicoId == null || ordemServicoId <= 0)
            throw new DomainValidationException("Ordem de serviço inválida");
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0)
            throw new DomainValidationException("O valor do pagamento deve ser maior que zero");
        if (valor.scale() > 2)
            throw new DomainValidationException("O valor do pagamento deve ter no máximo duas casas decimais");
        if (descricao == null || descricao.isBlank())
            throw new DomainValidationException("A descrição do pagamento é obrigatória");

        var agora = LocalDateTime.now();
        return new Pagamento(UUID.randomUUID(), ordemServicoId, valor.setScale(2, RoundingMode.UNNECESSARY),
                descricao.trim(), emailPagador, StatusPagamento.PENDENTE, null, null, null, null, agora, agora);
    }

    /* ================== FLUXO ================== */

    public void registrarPreferencia(String preferenciaId, String checkoutUrl) {
        if (preferenciaId == null || preferenciaId.isBlank() || checkoutUrl == null || checkoutUrl.isBlank())
            throw new DomainValidationException("Preferência do Mercado Pago inválida");
        this.preferenciaId = preferenciaId;
        this.checkoutUrl = checkoutUrl;
        touch();
    }

    /**
     * Aplica o status atual de um payment do Mercado Pago. Num mesmo checkout o
     * comprador pode tentar mais de uma vez (ex.: cartão recusado e depois PIX),
     * então REJEITADO/CANCELADO não são finais. Depois de APROVADO só aceitamos
     * REEMBOLSADO — uma notificação atrasada de uma tentativa recusada anterior
     * não pode "desaprovar" o pagamento.
     *
     * @return true se o status mudou
     */
    public boolean atualizarStatus(StatusPagamento novoStatus, Long mercadoPagoPaymentId, String statusDetalhe) {
        if (novoStatus == null)
            throw new DomainValidationException("Status do pagamento é obrigatório");
        if (this.status == StatusPagamento.REEMBOLSADO)
            return false;
        if (this.status == StatusPagamento.APROVADO && novoStatus != StatusPagamento.REEMBOLSADO)
            return false;
        if (this.status == novoStatus && java.util.Objects.equals(this.mercadoPagoPaymentId, mercadoPagoPaymentId))
            return false;

        this.status = novoStatus;
        this.mercadoPagoPaymentId = mercadoPagoPaymentId;
        this.statusDetalhe = statusDetalhe;
        touch();
        return true;
    }

    public boolean possuiCheckout() {
        return checkoutUrl != null;
    }

    public boolean isAprovado() {
        return status == StatusPagamento.APROVADO;
    }

    private void touch() {
        this.atualizadoEm = LocalDateTime.now();
    }

    /* ================== GETTERS ================== */

    public UUID getId() { return id; }
    public Long getOrdemServicoId() { return ordemServicoId; }
    public BigDecimal getValor() { return valor; }
    public String getDescricao() { return descricao; }
    public String getEmailPagador() { return emailPagador; }
    public StatusPagamento getStatus() { return status; }
    public String getPreferenciaId() { return preferenciaId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public Long getMercadoPagoPaymentId() { return mercadoPagoPaymentId; }
    public String getStatusDetalhe() { return statusDetalhe; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
