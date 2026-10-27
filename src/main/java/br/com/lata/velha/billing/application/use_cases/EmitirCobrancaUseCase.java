package br.com.lata.velha.billing.application.use_cases;

import br.com.lata.velha.billing.domain.events.CobrancaEmitidaEvent;
import br.com.lata.velha.billing.domain.exceptions.OrcamentoNotFoundException;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import br.com.lata.velha.shared.application.gateways.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmitirCobrancaUseCase {

    private static final Logger logger = LoggerFactory.getLogger(EmitirCobrancaUseCase.class);

    private final OrcamentoRepository orcamentoRepository;
    private final CriarPagamentoUseCase criarPagamentoUseCase;
    private final EventPublisher eventPublisher;

    public EmitirCobrancaUseCase(OrcamentoRepository orcamentoRepository,
                                 CriarPagamentoUseCase criarPagamentoUseCase,
                                 EventPublisher eventPublisher) {
        this.orcamentoRepository = orcamentoRepository;
        this.criarPagamentoUseCase = criarPagamentoUseCase;
        this.eventPublisher = eventPublisher;
    }

    public void execute(Long ordemServicoId) {
        var orcamento = orcamentoRepository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new OrcamentoNotFoundException(ordemServicoId));

        var descricao = "Cobrança OS #" + ordemServicoId;
        criarPagamentoUseCase.execute(ordemServicoId, orcamento.getValorTotal(), descricao, orcamento.getEmailCliente());

        eventPublisher.publish(new CobrancaEmitidaEvent(ordemServicoId));
        logger.info("Cobrança emitida - osId={}, valor={}", ordemServicoId, orcamento.getValorTotal());
    }
}
