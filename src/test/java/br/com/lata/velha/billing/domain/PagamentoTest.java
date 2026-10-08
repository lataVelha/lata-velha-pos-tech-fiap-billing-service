package br.com.lata.velha.billing.domain;

import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.billing.domain.enums.StatusPagamento;
import br.com.lata.velha.billing.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class PagamentoTest {

    private Pagamento novo() {
        return Pagamento.create(10L, new BigDecimal("150.50"), "Troca de óleo", "cliente@teste.com");
    }

    @Test
    void createDeveIniciarPendente() {
        var p = novo();
        assertThat(p.getId()).isNotNull();
        assertThat(p.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(p.getValor()).isEqualByComparingTo("150.50");
        assertThat(p.possuiCheckout()).isFalse();
    }

    @Test
    void createDeveValidarCampos() {
        assertThatThrownBy(() -> Pagamento.create(null, BigDecimal.TEN, "x", null)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Pagamento.create(1L, BigDecimal.ZERO, "x", null)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Pagamento.create(1L, new BigDecimal("1.001"), "x", null)).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Pagamento.create(1L, BigDecimal.TEN, " ", null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void registrarPreferencia() {
        var p = novo();
        p.registrarPreferencia("pref-1", "https://mp/checkout");
        assertThat(p.possuiCheckout()).isTrue();
        assertThat(p.getPreferenciaId()).isEqualTo("pref-1");
        assertThatThrownBy(() -> p.registrarPreferencia("", "x")).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejeitadoPodeSerSeguidoDeAprovado() {
        var p = novo();
        assertThat(p.atualizarStatus(StatusPagamento.REJEITADO, 1L, "cc_rejected_insufficient_amount")).isTrue();
        assertThat(p.atualizarStatus(StatusPagamento.APROVADO, 2L, "accredited")).isTrue();
        assertThat(p.isAprovado()).isTrue();
        assertThat(p.getMercadoPagoPaymentId()).isEqualTo(2L);
    }

    @Test
    void aprovadoNaoVoltaParaRejeitadoMasPodeSerReembolsado() {
        var p = novo();
        p.atualizarStatus(StatusPagamento.APROVADO, 2L, "accredited");
        assertThat(p.atualizarStatus(StatusPagamento.REJEITADO, 1L, "late")).isFalse();
        assertThat(p.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(p.atualizarStatus(StatusPagamento.REEMBOLSADO, 2L, "refunded")).isTrue();
        assertThat(p.atualizarStatus(StatusPagamento.APROVADO, 2L, "x")).isFalse();
    }

    @Test
    void mesmoStatusMesmoPaymentNaoMuda() {
        var p = novo();
        assertThat(p.atualizarStatus(StatusPagamento.PENDENTE, 5L, null)).isTrue();
        assertThat(p.atualizarStatus(StatusPagamento.PENDENTE, 5L, null)).isFalse();
        assertThatThrownBy(() -> p.atualizarStatus(null, 5L, null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void mapeiaStatusDoMercadoPago() {
        assertThat(StatusPagamento.fromMercadoPago("pending")).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(StatusPagamento.fromMercadoPago("in_process")).isEqualTo(StatusPagamento.EM_PROCESSAMENTO);
        assertThat(StatusPagamento.fromMercadoPago("authorized")).isEqualTo(StatusPagamento.EM_PROCESSAMENTO);
        assertThat(StatusPagamento.fromMercadoPago("approved")).isEqualTo(StatusPagamento.APROVADO);
        assertThat(StatusPagamento.fromMercadoPago("rejected")).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(StatusPagamento.fromMercadoPago("cancelled")).isEqualTo(StatusPagamento.CANCELADO);
        assertThat(StatusPagamento.fromMercadoPago("charged_back")).isEqualTo(StatusPagamento.REEMBOLSADO);
        assertThatThrownBy(() -> StatusPagamento.fromMercadoPago("xyz")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StatusPagamento.fromMercadoPago(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
