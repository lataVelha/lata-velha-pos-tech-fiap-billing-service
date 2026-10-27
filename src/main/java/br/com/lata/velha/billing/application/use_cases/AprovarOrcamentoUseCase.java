package br.com.lata.velha.billing.application.use_cases;

import br.com.lata.velha.billing.domain.events.OrcamentoAprovadoEvent;
import br.com.lata.velha.billing.domain.exceptions.OrcamentoNotFoundException;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import br.com.lata.velha.shared.application.gateways.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AprovarOrcamentoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(AprovarOrcamentoUseCase.class);

    private final OrcamentoRepository orcamentoRepository;
    private final EventPublisher eventPublisher;

    public AprovarOrcamentoUseCase(OrcamentoRepository orcamentoRepository, EventPublisher eventPublisher) {
        this.orcamentoRepository = orcamentoRepository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(Long ordemServicoId) {
        var orcamento = orcamentoRepository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new OrcamentoNotFoundException(ordemServicoId));

        orcamento.aprovar();
        orcamentoRepository.salvar(orcamento);

        eventPublisher.publish(new OrcamentoAprovadoEvent(ordemServicoId, orcamento.getValorTotal()));
        logger.info("Orçamento aprovado - osId={}, valorTotal={}", ordemServicoId, orcamento.getValorTotal());
    }
}
