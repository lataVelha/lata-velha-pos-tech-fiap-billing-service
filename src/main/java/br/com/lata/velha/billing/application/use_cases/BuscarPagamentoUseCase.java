package br.com.lata.velha.billing.application.use_cases;

import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.billing.domain.exceptions.PagamentoNotFoundException;
import br.com.lata.velha.billing.domain.repositories.PagamentoRepository;

import java.util.UUID;

public class BuscarPagamentoUseCase {

    private final PagamentoRepository repository;

    public BuscarPagamentoUseCase(PagamentoRepository repository) {
        this.repository = repository;
    }

    public Pagamento porId(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new PagamentoNotFoundException("Pagamento não encontrado: " + id));
    }

    public Pagamento porOrdemServico(Long ordemServicoId) {
        return repository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new PagamentoNotFoundException("Nenhum pagamento para a ordem de serviço: " + ordemServicoId));
    }
}
