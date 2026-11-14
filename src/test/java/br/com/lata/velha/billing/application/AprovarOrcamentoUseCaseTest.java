package br.com.lata.velha.billing.application;

import br.com.lata.velha.billing.application.use_cases.AprovarOrcamentoUseCase;
import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.enums.StatusOrcamento;
import br.com.lata.velha.billing.domain.events.OrcamentoAprovadoEvent;
import br.com.lata.velha.billing.domain.exceptions.OrcamentoNotFoundException;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import br.com.lata.velha.shared.application.gateways.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AprovarOrcamentoUseCaseTest {

    private OrcamentoRepository repository;
    private EventPublisher eventPublisher;
    private AprovarOrcamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(OrcamentoRepository.class);
        eventPublisher = mock(EventPublisher.class);
        when(repository.salvar(any())).thenAnswer(i -> i.getArgument(0));
        useCase = new AprovarOrcamentoUseCase(repository, eventPublisher);
    }

    @Test
    void aprovaOrcamentoPendenteEPublicaEvento() {
        var orcamento = Orcamento.create(10L, new BigDecimal("500.00"), "cliente@teste.com", List.of());
        when(repository.buscarPorOrdemServicoId(10L)).thenReturn(Optional.of(orcamento));

        useCase.execute(10L);

        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.APROVADO);
        verify(repository).salvar(orcamento);
        verify(eventPublisher).publish(any(OrcamentoAprovadoEvent.class));
    }

    @Test
    void lancaExcecaoQuandoOrcamentoNaoEncontrado() {
        when(repository.buscarPorOrdemServicoId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L))
                .isInstanceOf(OrcamentoNotFoundException.class);

        verifyNoInteractions(eventPublisher);
    }
}
