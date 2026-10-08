package br.com.lata.velha.billing.infrastructure.persistence;

import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.billing.domain.repositories.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PagamentoRepositoryImpl implements PagamentoRepository {

    private final PagamentoJpaRepository jpa;

    @Override
    public Pagamento salvar(Pagamento pagamento) {
        return jpa.save(PagamentoEntity.fromDomain(pagamento)).toDomain();
    }

    @Override
    public Optional<Pagamento> buscarPorId(UUID id) {
        return jpa.findById(id).map(PagamentoEntity::toDomain);
    }

    @Override
    public Optional<Pagamento> buscarPorOrdemServicoId(Long ordemServicoId) {
        return jpa.findByOrdemServicoId(ordemServicoId).map(PagamentoEntity::toDomain);
    }
}
