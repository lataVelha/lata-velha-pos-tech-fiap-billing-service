package br.com.lata.velha.billing.domain.exceptions;

public class DomainValidationException extends DomainException {
    public DomainValidationException(String message) {
        super(message);
    }
}
