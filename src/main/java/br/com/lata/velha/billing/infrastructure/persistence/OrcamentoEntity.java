package br.com.lata.velha.billing.infrastructure.persistence;

import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.enums.StatusOrcamento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orcamento")
public class OrcamentoEntity {

    @Id
    private UUID id;

    @Column(name = "ordem_servico_id", nullable = false, unique = true)
    private Long ordemServicoId;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "email_cliente")
    private String emailCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrcamento status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public static OrcamentoEntity from(Orcamento orcamento) {
        var entity = new OrcamentoEntity();
        entity.id = orcamento.getId();
        entity.ordemServicoId = orcamento.getOrdemServicoId();
        entity.valorTotal = orcamento.getValorTotal();
        entity.emailCliente = orcamento.getEmailCliente();
        entity.status = orcamento.getStatus();
        entity.criadoEm = orcamento.getCriadoEm();
        entity.atualizadoEm = orcamento.getAtualizadoEm();
        return entity;
    }

    public Orcamento toDomain() {
        return new Orcamento(id, ordemServicoId, valorTotal, emailCliente, status, criadoEm, atualizadoEm);
    }
}
