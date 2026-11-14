package br.com.lata.velha.billing.application.use_cases;

import br.com.lata.velha.billing.domain.exceptions.OrcamentoNotFoundException;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AprovarOrcamentoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(AprovarOrcamentoUseCase.class);

    private final OrcamentoRepository orcamentoRepository;

    public AprovarOrcamentoUseCase(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    public void execute(Long ordemServicoId) {
        var orcamento = orcamentoRepository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new OrcamentoNotFoundException(ordemServicoId));

        orcamento.aprovar();
        orcamentoRepository.salvar(orcamento);
        logger.info("Orçamento aprovado - osId={}", ordemServicoId);
    }
}
