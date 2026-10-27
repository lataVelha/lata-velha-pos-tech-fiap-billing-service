package br.com.lata.velha.billing.domain.entities;

import br.com.lata.velha.billing.domain.enums.StatusOrcamento;
import br.com.lata.velha.billing.domain.exceptions.DomainValidationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class Orcamento {

    private final UUID id;
    private final Long ordemServicoId;
    private final BigDecimal valorTotal;
    private final String emailCliente;
    private StatusOrcamento status;
    private final LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Orcamento(UUID id, Long ordemServicoId, BigDecimal valorTotal, String emailCliente,
                     StatusOrcamento status, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.ordemServicoId = ordemServicoId;
        this.valorTotal = valorTotal;
        this.emailCliente = emailCliente;
        this.status = status;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public static Orcamento create(Long ordemServicoId, BigDecimal valorTotal, String emailCliente) {
        if (ordemServicoId == null || ordemServicoId <= 0)
            throw new DomainValidationException("Ordem de serviço inválida");
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) < 0)
            throw new DomainValidationException("Valor do orçamento não pode ser negativo");

        var agora = LocalDateTime.now();
        return new Orcamento(UUID.randomUUID(), ordemServicoId, valorTotal, emailCliente,
                StatusOrcamento.PENDENTE, agora, agora);
    }

    public void aprovar() {
        if (status != StatusOrcamento.PENDENTE)
            throw new DomainValidationException("Orçamento não está pendente de aprovação");
        this.status = StatusOrcamento.APROVADO;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void reprovar() {
        if (status != StatusOrcamento.PENDENTE)
            throw new DomainValidationException("Orçamento não está pendente de aprovação");
        this.status = StatusOrcamento.REPROVADO;
        this.atualizadoEm = LocalDateTime.now();
    }

    public boolean isPendente() { return status == StatusOrcamento.PENDENTE; }
    public boolean isAprovado() { return status == StatusOrcamento.APROVADO; }

    public UUID getId() { return id; }
    public Long getOrdemServicoId() { return ordemServicoId; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public String getEmailCliente() { return emailCliente; }
    public StatusOrcamento getStatus() { return status; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
}
