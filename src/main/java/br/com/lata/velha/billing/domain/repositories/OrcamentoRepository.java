package br.com.lata.velha.billing.domain.repositories;

import br.com.lata.velha.billing.domain.entities.Orcamento;

import java.util.Optional;

public interface OrcamentoRepository {
    Orcamento salvar(Orcamento orcamento);
    Optional<Orcamento> buscarPorOrdemServicoId(Long ordemServicoId);
}
