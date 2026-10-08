package br.com.lata.velha.billing.domain.exceptions;

public class PagamentoJaAprovadoException extends DomainException {
    public PagamentoJaAprovadoException(Long ordemServicoId) {
        super("A ordem de serviço " + ordemServicoId + " já possui pagamento aprovado");
    }
}
