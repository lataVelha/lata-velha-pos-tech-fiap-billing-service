package br.com.lata.velha.billing.domain.exceptions;

public class PagamentoNotFoundException extends DomainException {
    public PagamentoNotFoundException(String message) {
        super(message);
    }
}
