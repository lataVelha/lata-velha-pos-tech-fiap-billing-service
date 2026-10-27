package br.com.lata.velha.billing.domain.exceptions;

public class OrcamentoNotFoundException extends DomainException {
    public OrcamentoNotFoundException(Long ordemServicoId) {
        super("Orçamento não encontrado para a OS " + ordemServicoId);
    }
}
