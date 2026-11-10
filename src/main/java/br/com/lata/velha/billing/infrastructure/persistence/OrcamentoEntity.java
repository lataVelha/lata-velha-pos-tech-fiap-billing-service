package br.com.lata.velha.billing.infrastructure.persistence;

import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.entities.OrcamentoItem;
import br.com.lata.velha.billing.domain.enums.StatusOrcamento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "orcamento_item", joinColumns = @JoinColumn(name = "orcamento_id"))
    private List<OrcamentoItemEmbeddable> itens;

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
        entity.itens = orcamento.getItens().stream().map(OrcamentoItemEmbeddable::from).toList();
        entity.status = orcamento.getStatus();
        entity.criadoEm = orcamento.getCriadoEm();
        entity.atualizadoEm = orcamento.getAtualizadoEm();
        return entity;
    }

    public Orcamento toDomain() {
        var domainItens = itens == null
                ? List.<OrcamentoItem>of()
                : itens.stream().map(OrcamentoItemEmbeddable::toDomain).toList();
        return new Orcamento(id, ordemServicoId, valorTotal, emailCliente, domainItens,
                status, criadoEm, atualizadoEm);
    }
}
