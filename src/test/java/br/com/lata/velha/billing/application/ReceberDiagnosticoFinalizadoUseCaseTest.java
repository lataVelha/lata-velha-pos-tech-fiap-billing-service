package br.com.lata.velha.billing.application;

import br.com.lata.velha.billing.application.gateways.InventoryGateway;
import br.com.lata.velha.billing.application.use_cases.ReceberDiagnosticoFinalizadoUseCase;
import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.entities.OrcamentoItem;
import br.com.lata.velha.billing.domain.events.DiagnosticoFinalizadoEvent;
import br.com.lata.velha.billing.domain.events.DiagnosticoFinalizadoEvent.ItemDiagnostico;
import br.com.lata.velha.billing.domain.events.DiagnosticoFinalizadoEvent.ItemPeca;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReceberDiagnosticoFinalizadoUseCaseTest {

    private OrcamentoRepository repository;
    private InventoryGateway inventoryGateway;
    private JavaMailSender mailSender;
    private TemplateEngine templateEngine;
    private ReceberDiagnosticoFinalizadoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(OrcamentoRepository.class);
        inventoryGateway = mock(InventoryGateway.class);
        mailSender = mock(JavaMailSender.class);
        templateEngine = mock(TemplateEngine.class);
        useCase = new ReceberDiagnosticoFinalizadoUseCase(
                repository, inventoryGateway, mailSender, templateEngine, "noreply@latavelha.com.br");
        when(repository.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void ignoraEventoQuandoJaExisteOrcamentoParaOs() {
        var existente = Orcamento.create(10L, BigDecimal.TEN, "cliente@teste.com", List.of());
        when(repository.buscarPorOrdemServicoId(10L)).thenReturn(Optional.of(existente));

        useCase.execute(new DiagnosticoFinalizadoEvent(10L, List.of()));

        verify(repository, never()).salvar(any());
        verifyNoInteractions(inventoryGateway, mailSender, templateEngine);
    }

    @Test
    void criaOrcamentoSemItens() {
        when(repository.buscarPorOrdemServicoId(11L)).thenReturn(Optional.empty());

        useCase.execute(new DiagnosticoFinalizadoEvent(11L, List.of()));

        var captor = ArgumentCaptor.forClass(Orcamento.class);
        verify(repository).salvar(captor.capture());
        assertThat(captor.getValue().getOrdemServicoId()).isEqualTo(11L);
        assertThat(captor.getValue().getValorTotal()).isEqualByComparingTo("0");
        assertThat(captor.getValue().getItens()).isEmpty();
        verifyNoInteractions(inventoryGateway, mailSender);
    }

    @Test
    void calculaTotalComMaoDeObraEPecasUsandoInventory() {
        var event = new DiagnosticoFinalizadoEvent(12L, List.of(
                new ItemDiagnostico(100L, 1L, new BigDecimal("120.00"),
                        List.of(new ItemPeca(7L, 2))),
                new ItemDiagnostico(101L, 2L, null,
                        List.of(new ItemPeca(8L, 3), new ItemPeca(9L, 1))),
                new ItemDiagnostico(102L, 3L, new BigDecimal("50.00"), List.of())
        ));
        when(repository.buscarPorOrdemServicoId(12L)).thenReturn(Optional.empty());
        when(inventoryGateway.buscarValorPeca(7L)).thenReturn(Optional.of(new BigDecimal("10.00")));
        when(inventoryGateway.buscarValorPeca(8L)).thenReturn(Optional.empty());
        when(inventoryGateway.buscarValorPeca(9L)).thenReturn(Optional.of(new BigDecimal("5.50")));

        useCase.execute(event);

        var captor = ArgumentCaptor.forClass(Orcamento.class);
        verify(repository).salvar(captor.capture());
        // 120 + (10 * 2) + 0 + (0 * 3) + (5.50 * 1) + 50
        assertThat(captor.getValue().getValorTotal()).isEqualByComparingTo("195.50");
        assertThat(captor.getValue().getItens())
                .containsExactly(
                        new OrcamentoItem(100L, 7L, 2),
                        new OrcamentoItem(101L, 8L, 3),
                        new OrcamentoItem(101L, 9L, 1));
    }

    @Test
    void enviaEmailQuandoClientePossuiEmail() {
        when(repository.buscarPorOrdemServicoId(13L)).thenAnswer(invocation -> {
            return Optional.empty();
        });
        when(repository.salvar(any())).thenAnswer(invocation -> Orcamento.create(
                13L, new BigDecimal("99.90"), "cliente@teste.com", List.of()));
        when(templateEngine.process(eq("os-notificacao"), any(Context.class))).thenReturn("<html>orcamento</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        useCase.execute(new DiagnosticoFinalizadoEvent(13L, List.of()));

        verify(templateEngine).process(eq("os-notificacao"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void naoEnviaEmailQuandoEmailDoClienteEstaEmBranco() {
        when(repository.buscarPorOrdemServicoId(14L)).thenReturn(Optional.empty());
        when(repository.salvar(any())).thenAnswer(invocation -> Orcamento.create(
                14L, BigDecimal.TEN, "   ", List.of()));

        useCase.execute(new DiagnosticoFinalizadoEvent(14L, List.of()));

        verifyNoInteractions(templateEngine, mailSender);
    }

    @Test
    void falhaNoEnvioDeEmailNaoPropagaErro() {
        when(repository.buscarPorOrdemServicoId(15L)).thenReturn(Optional.empty());
        when(repository.salvar(any())).thenAnswer(invocation -> Orcamento.create(
                15L, BigDecimal.TEN, "cliente@teste.com", List.of()));
        when(mailSender.createMimeMessage()).thenThrow(new IllegalStateException("SMTP indisponível"));

        assertThatCode(() -> useCase.execute(new DiagnosticoFinalizadoEvent(15L, List.of())))
                .doesNotThrowAnyException();
    }
}
