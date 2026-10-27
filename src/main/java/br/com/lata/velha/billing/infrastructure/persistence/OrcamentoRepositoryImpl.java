package br.com.lata.velha.billing.infrastructure.persistence;

import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OrcamentoRepositoryImpl implements OrcamentoRepository {

    private final OrcamentoJpaRepository jpa;

    public OrcamentoRepositoryImpl(OrcamentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Orcamento salvar(Orcamento orcamento) {
        return jpa.save(OrcamentoEntity.from(orcamento)).toDomain();
    }

    @Override
    public Optional<Orcamento> buscarPorOrdemServicoId(Long ordemServicoId) {
        return jpa.findByOrdemServicoId(ordemServicoId).map(OrcamentoEntity::toDomain);
    }
}
