package br.com.lata.velha.billing.api.handlers;

import br.com.lata.velha.billing.domain.exceptions.DomainValidationException;
import br.com.lata.velha.billing.domain.exceptions.IntegracaoPagamentoException;
import br.com.lata.velha.billing.domain.exceptions.OrcamentoNotFoundException;
import br.com.lata.velha.billing.domain.exceptions.PagamentoJaAprovadoException;
import br.com.lata.velha.billing.domain.exceptions.PagamentoNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapeiaPagamentoNaoEncontradoPara404() {
        var problem = handler.notFound(new PagamentoNotFoundException("não achou"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).isEqualTo("não achou");
    }

    @Test
    void mapeiaOrcamentoNaoEncontradoPara404() {
        var problem = handler.orcamentoNotFound(new OrcamentoNotFoundException(1L));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).contains("OS 1");
    }

    @Test
    void mapeiaValidacaoPara422() {
        var problem = handler.validacao(new DomainValidationException("valor inválido"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getDetail()).isEqualTo("valor inválido");
    }

    @Test
    void mapeiaRequestInvalidoPara400ComCampos() {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new org.springframework.validation.FieldError("request", "valor", "deve ser positivo"));
        var problem = handler.requestInvalido(new MethodArgumentNotValidException(null, binding));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).contains("valor").contains("deve ser positivo");
    }

    @Test
    void mapeiaPagamentoJaAprovadoPara409() {
        var problem = handler.jaAprovado(new PagamentoJaAprovadoException(2L));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getDetail()).contains("serviço 2");
    }

    @Test
    void mapeiaViolacaoDeIntegridadePara409() {
        var problem = handler.conflito(new DataIntegrityViolationException(
                "duplicado", new RuntimeException("unique constraint")));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getDetail()).contains("Pagamento já está sendo criado");
    }

    @Test
    void mapeiaFalhaDeIntegracaoPara502() {
        var problem = handler.integracao(new IntegracaoPagamentoException("fora do ar", null));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(problem.getDetail()).isEqualTo("fora do ar");
    }
}
