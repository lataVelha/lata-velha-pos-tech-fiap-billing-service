package br.com.lata.velha.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrcamentoJpaRepository extends JpaRepository<OrcamentoEntity, UUID> {
    Optional<OrcamentoEntity> findByOrdemServicoId(Long ordemServicoId);
}
