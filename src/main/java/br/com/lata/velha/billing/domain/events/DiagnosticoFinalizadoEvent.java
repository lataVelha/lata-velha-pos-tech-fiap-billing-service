package br.com.lata.velha.billing.domain.events;

import br.com.lata.velha.shared.domain.events.AbstractDomainEvent;

import java.math.BigDecimal;
import java.util.List;

public final class DiagnosticoFinalizadoEvent extends AbstractDomainEvent {

    private final Long osId;
    private final List<ItemDiagnostico> itens;

    public DiagnosticoFinalizadoEvent(Long osId, List<ItemDiagnostico> itens) {
        this.osId = osId;
        this.itens = itens;
    }

    @Override
    public String eventType() {
        return "DiagnosticoFinalizado";
    }

    public Long getOsId() { return osId; }
    public List<ItemDiagnostico> getItens() { return itens; }

    public record ItemDiagnostico(Long execucaoServicoId, Long servicoId, BigDecimal valorMaoDeObra, List<ItemPeca> pecas) {}
    public record ItemPeca(Long pecaId, Integer quantidade) {}
}
