package br.com.lata.velha.billing.application;

import br.com.lata.velha.billing.application.use_cases.CriarPagamentoUseCase;
import br.com.lata.velha.billing.application.use_cases.EmitirCobrancaUseCase;
import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.shared.domain.events.CobrancaEmitidaEvent;
import br.com.lata.velha.billing.domain.exceptions.OrcamentoNotFoundException;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import br.com.lata.velha.shared.application.gateways.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EmitirCobrancaUseCaseTest {

    private OrcamentoRepository repository;
    private CriarPagamentoUseCase criarPagamentoUseCase;
    private EventPublisher eventPublisher;
    private JavaMailSender mailSender;
    private TemplateEngine templateEngine;
    private EmitirCobrancaUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(OrcamentoRepository.class);
        criarPagamentoUseCase = mock(CriarPagamentoUseCase.class);
        eventPublisher = mock(EventPublisher.class);
        mailSender = mock(JavaMailSender.class);
        templateEngine = mock(TemplateEngine.class);
        useCase = new EmitirCobrancaUseCase(repository, criarPagamentoUseCase, eventPublisher, mailSender, templateEngine, "noreply@latavelha.com.br");

        var pagamento = mock(Pagamento.class);
        when(pagamento.getCheckoutUrl()).thenReturn("https://mp/checkout");
        when(criarPagamentoUseCase.execute(any(), any(), any(), any()))
                .thenReturn(new CriarPagamentoUseCase.Resultado(pagamento, true));
    }

    @Test
    void emiteCobrancaEPublicaEvento() {
        var orcamento = Orcamento.create(10L, new BigDecimal("350.00"), "cliente@teste.com", List.of());
        when(repository.buscarPorOrdemServicoId(10L)).thenReturn(Optional.of(orcamento));

        useCase.execute(10L);

        verify(criarPagamentoUseCase).execute(
                10L, new BigDecimal("350.00"), "Cobrança OS #10", "cliente@teste.com");
        var captor = ArgumentCaptor.forClass(CobrancaEmitidaEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getOsId()).isEqualTo(10L);
        assertThat(captor.getValue().eventType()).isEqualTo("CobrancaEmitida");
    }

    @Test
    void lancaExcecaoQuandoOrcamentoNaoExiste() {
        when(repository.buscarPorOrdemServicoId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L))
                .isInstanceOf(OrcamentoNotFoundException.class);

        verifyNoInteractions(eventPublisher);
    }
}
