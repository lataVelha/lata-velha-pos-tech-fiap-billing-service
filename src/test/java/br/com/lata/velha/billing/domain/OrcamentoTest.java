package br.com.lata.velha.billing.domain;

import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.entities.OrcamentoItem;
import br.com.lata.velha.billing.domain.enums.StatusOrcamento;
import br.com.lata.velha.billing.domain.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrcamentoTest {

    @Test
    void criaOrcamentoPendenteSemItens() {
        var orcamento = Orcamento.create(1L, new BigDecimal("100.00"), "cliente@teste.com", null);

        assertThat(orcamento.getId()).isNotNull();
        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.PENDENTE);
        assertThat(orcamento.isPendente()).isTrue();
        assertThat(orcamento.isAprovado()).isFalse();
        assertThat(orcamento.getItens()).isEmpty();
        assertThat(orcamento.getCriadoEm()).isNotNull();
        assertThat(orcamento.getAtualizadoEm()).isNotNull();
    }

    @Test
    void validaOrdemServico() {
        assertThatThrownBy(() -> Orcamento.create(null, BigDecimal.TEN, null, List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Ordem de serviço inválida");
        assertThatThrownBy(() -> Orcamento.create(0L, BigDecimal.TEN, null, List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Ordem de serviço inválida");
    }

    @Test
    void validaValorTotal() {
        assertThatThrownBy(() -> Orcamento.create(1L, null, null, List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Valor do orçamento não pode ser negativo");
        assertThatThrownBy(() -> Orcamento.create(1L, new BigDecimal("-1"), null, List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Valor do orçamento não pode ser negativo");
    }

    @Test
    void aprovaSomenteQuandoPendente() {
        var orcamento = Orcamento.create(1L, BigDecimal.TEN, null,
                List.of(new OrcamentoItem(1L, 2L, 3)));

        orcamento.aprovar();

        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.APROVADO);
        assertThat(orcamento.isAprovado()).isTrue();
        assertThat(orcamento.isPendente()).isFalse();
        assertThatThrownBy(orcamento::aprovar).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void reprovaSomenteQuandoPendente() {
        var orcamento = Orcamento.create(1L, BigDecimal.TEN, null, List.of());

        orcamento.reprovar();

        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.REPROVADO);
        assertThatThrownBy(orcamento::reprovar).isInstanceOf(DomainValidationException.class);
    }
}
