package br.com.lata.velha.billing.infrastructure;

import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.entities.OrcamentoItem;
import br.com.lata.velha.billing.infrastructure.persistence.OrcamentoEntity;
import br.com.lata.velha.billing.infrastructure.persistence.OrcamentoItemEmbeddable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrcamentoItemEmbeddableTest {

    @Test
    void converteIdaEVoltaComDominio() {
        var item = new OrcamentoItem(1L, 2L, 3);

        var embeddable = OrcamentoItemEmbeddable.from(item);

        assertThat(embeddable.toDomain()).isEqualTo(item);
    }

    @Test
    void equalsEHashCodeConsideramTodosOsCampos() {
        var a = OrcamentoItemEmbeddable.from(new OrcamentoItem(1L, 2L, 3));
        var b = OrcamentoItemEmbeddable.from(new OrcamentoItem(1L, 2L, 3));
        var diferenteExecucao = OrcamentoItemEmbeddable.from(new OrcamentoItem(9L, 2L, 3));
        var diferentePeca = OrcamentoItemEmbeddable.from(new OrcamentoItem(1L, 9L, 3));
        var diferenteQuantidade = OrcamentoItemEmbeddable.from(new OrcamentoItem(1L, 2L, 9));

        assertThat(a).isEqualTo(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(diferenteExecucao)
                .isNotEqualTo(diferentePeca)
                .isNotEqualTo(diferenteQuantidade)
                .isNotEqualTo(null)
                .isNotEqualTo("outro tipo");
    }

    @Test
    void construtorProtegidoParaJpa() throws Exception {
        Constructor<OrcamentoItemEmbeddable> constructor =
                OrcamentoItemEmbeddable.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThat(constructor.newInstance()).isNotNull();
    }

    @Test
    void orcamentoEntityConverteItensNulosParaListaVazia() throws Exception {
        var orcamento = new Orcamento(UUID.randomUUID(), 1L, BigDecimal.TEN, null, List.of(),
                br.com.lata.velha.billing.domain.enums.StatusOrcamento.PENDENTE,
                LocalDateTime.now(), LocalDateTime.now());
        var entity = OrcamentoEntity.from(orcamento);

        var field = OrcamentoEntity.class.getDeclaredField("itens");
        field.setAccessible(true);
        field.set(entity, null);

        assertThat(entity.toDomain().getItens()).isEmpty();
    }
}
