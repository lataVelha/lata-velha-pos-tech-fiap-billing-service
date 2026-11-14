package br.com.lata.velha.billing.application.use_cases;

import br.com.lata.velha.billing.domain.events.OrcamentoReprovadoEvent;
import br.com.lata.velha.billing.domain.exceptions.OrcamentoNotFoundException;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import br.com.lata.velha.shared.application.gateways.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReprovarOrcamentoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ReprovarOrcamentoUseCase.class);

    private final OrcamentoRepository orcamentoRepository;
    private final EventPublisher eventPublisher;

    public ReprovarOrcamentoUseCase(OrcamentoRepository orcamentoRepository, EventPublisher eventPublisher) {
        this.orcamentoRepository = orcamentoRepository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(Long ordemServicoId) {
        var orcamento = orcamentoRepository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new OrcamentoNotFoundException(ordemServicoId));

        orcamento.reprovar();
        orcamentoRepository.salvar(orcamento);

        eventPublisher.publish(new OrcamentoReprovadoEvent(ordemServicoId));
        logger.info("Orçamento reprovado pelo cliente - osId={}", ordemServicoId);
    }
}
