package br.com.lata.velha.billing.application;

import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway;
import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway.PagamentoMercadoPago;
import br.com.lata.velha.billing.application.use_cases.ProcessarNotificacaoUseCase;
import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.billing.domain.enums.StatusPagamento;
import br.com.lata.velha.billing.domain.repositories.PagamentoRepository;
import br.com.lata.velha.shared.application.gateways.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import br.com.lata.velha.billing.domain.events.PaymentDoneEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProcessarNotificacaoUseCaseTest {

    private PagamentoRepository repository;
    private MercadoPagoGateway mercadoPago;
    private EventPublisher eventPublisher;
    private ProcessarNotificacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(PagamentoRepository.class);
        mercadoPago = mock(MercadoPagoGateway.class);
        eventPublisher = mock(EventPublisher.class);
        useCase = new ProcessarNotificacaoUseCase(repository, mercadoPago, eventPublisher);
    }

    @Test
    void atualizaStatusQuandoAprovado() {
        var pagamento = Pagamento.create(1L, BigDecimal.TEN, "x", null);
        when(mercadoPago.buscarPagamento(55L))
                .thenReturn(new PagamentoMercadoPago(55L, "approved", "accredited", pagamento.getId().toString()));
        when(repository.buscarPorId(pagamento.getId())).thenReturn(Optional.of(pagamento));

        useCase.execute(55L);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(pagamento.getMercadoPagoPaymentId()).isEqualTo(55L);
        verify(repository).salvar(pagamento);
        verify(eventPublisher).publish(any(PaymentDoneEvent.class));
    }

    @Test
    void naoSalvaQuandoNadaMudou() {
        var pagamento = Pagamento.create(1L, BigDecimal.TEN, "x", null);
        pagamento.atualizarStatus(StatusPagamento.APROVADO, 55L, "accredited");
        when(mercadoPago.buscarPagamento(55L))
                .thenReturn(new PagamentoMercadoPago(55L, "approved", "accredited", pagamento.getId().toString()));
        when(repository.buscarPorId(pagamento.getId())).thenReturn(Optional.of(pagamento));

        useCase.execute(55L);

        verify(repository, never()).salvar(any());
    }

    @Test
    void ignoraReferenciaExternaInvalidaOuDesconhecida() {
        when(mercadoPago.buscarPagamento(1L)).thenReturn(new PagamentoMercadoPago(1L, "approved", null, "nao-uuid"));
        when(mercadoPago.buscarPagamento(2L)).thenReturn(new PagamentoMercadoPago(2L, "approved", null, null));
        var desconhecido = UUID.randomUUID();
        when(mercadoPago.buscarPagamento(3L)).thenReturn(new PagamentoMercadoPago(3L, "approved", null, desconhecido.toString()));
        when(repository.buscarPorId(desconhecido)).thenReturn(Optional.empty());

        useCase.execute(1L);
        useCase.execute(2L);
        useCase.execute(3L);

        verify(repository, never()).salvar(any());
    }
}
