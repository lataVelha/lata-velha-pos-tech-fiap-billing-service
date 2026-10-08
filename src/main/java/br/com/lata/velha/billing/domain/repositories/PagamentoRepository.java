package br.com.lata.velha.billing.domain.repositories;

import br.com.lata.velha.billing.domain.entities.Pagamento;

import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository {
    Pagamento salvar(Pagamento pagamento);
    Optional<Pagamento> buscarPorId(UUID id);
    Optional<Pagamento> buscarPorOrdemServicoId(Long ordemServicoId);
}
