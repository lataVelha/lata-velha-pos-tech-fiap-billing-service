package br.com.lata.velha.billing.api.handlers;

import br.com.lata.velha.billing.domain.exceptions.DomainValidationException;
import br.com.lata.velha.billing.domain.exceptions.IntegracaoPagamentoException;
import br.com.lata.velha.billing.domain.exceptions.PagamentoJaAprovadoException;
import br.com.lata.velha.billing.domain.exceptions.PagamentoNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PagamentoNotFoundException.class)
    public ProblemDetail notFound(PagamentoNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail validacao(DomainValidationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail requestInvalido(MethodArgumentNotValidException e) {
        var detalhe = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
    }

    @ExceptionHandler(PagamentoJaAprovadoException.class)
    public ProblemDetail jaAprovado(PagamentoJaAprovadoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Duas criações simultâneas para a mesma OS — a segunda bate na unique key. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflito(DataIntegrityViolationException e) {
        logger.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Pagamento já está sendo criado para esta ordem de serviço");
    }

    @ExceptionHandler(IntegracaoPagamentoException.class)
    public ProblemDetail integracao(IntegracaoPagamentoException e) {
        logger.error("Erro na integração com o Mercado Pago: {}", e.getMessage(), e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }
}
