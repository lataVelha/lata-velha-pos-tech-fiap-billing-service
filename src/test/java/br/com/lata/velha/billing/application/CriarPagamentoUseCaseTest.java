package br.com.lata.velha.billing.application;

import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway;
import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway.PreferenciaCriada;
import br.com.lata.velha.billing.application.use_cases.CriarPagamentoUseCase;
import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.billing.domain.enums.StatusPagamento;
import br.com.lata.velha.billing.domain.exceptions.PagamentoJaAprovadoException;
import br.com.lata.velha.billing.domain.repositories.PagamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CriarPagamentoUseCaseTest {

    private PagamentoRepository repository;
    private MercadoPagoGateway mercadoPago;
    private CriarPagamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(PagamentoRepository.class);
        mercadoPago = mock(MercadoPagoGateway.class);
        when(repository.salvar(any())).thenAnswer(i -> i.getArgument(0));
        when(mercadoPago.criarPreferencia(any())).thenReturn(new PreferenciaCriada("pref", "https://checkout"));
        useCase = new CriarPagamentoUseCase(repository, mercadoPago);
    }

    @Test
    void criaPagamentoEPreferencia() {
        when(repository.buscarPorOrdemServicoId(1L)).thenReturn(Optional.empty());

        var resultado = useCase.execute(1L, new BigDecimal("50.00"), "Alinhamento", null);

        assertThat(resultado.criado()).isTrue();
        assertThat(resultado.pagamento().getCheckoutUrl()).isEqualTo("https://checkout");
        verify(repository, times(2)).salvar(any());
    }

    @Test
    void reaproveitaPagamentoExistenteComCheckout() {
        var existente = Pagamento.create(1L, BigDecimal.TEN, "x", null);
        existente.registrarPreferencia("p", "https://ja-existe");
        when(repository.buscarPorOrdemServicoId(1L)).thenReturn(Optional.of(existente));

        var resultado = useCase.execute(1L, BigDecimal.TEN, "x", null);

        assertThat(resultado.criado()).isFalse();
        assertThat(resultado.pagamento().getCheckoutUrl()).isEqualTo("https://ja-existe");
        verifyNoInteractions(mercadoPago);
    }

    @Test
    void refazPreferenciaSeCriacaoAnteriorFalhou() {
        var existente = Pagamento.create(1L, BigDecimal.TEN, "x", null);
        when(repository.buscarPorOrdemServicoId(1L)).thenReturn(Optional.of(existente));

        var resultado = useCase.execute(1L, BigDecimal.TEN, "x", null);

        assertThat(resultado.pagamento().getCheckoutUrl()).isEqualTo("https://checkout");
        verify(mercadoPago).criarPreferencia(existente);
    }

    @Test
    void falhaSeOsJaFoiPaga() {
        var existente = Pagamento.create(1L, BigDecimal.TEN, "x", null);
        existente.atualizarStatus(StatusPagamento.APROVADO, 9L, "accredited");
        when(repository.buscarPorOrdemServicoId(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> useCase.execute(1L, BigDecimal.TEN, "x", null))
                .isInstanceOf(PagamentoJaAprovadoException.class);
    }
}
