package br.com.lata.velha.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PagamentoJpaRepository extends JpaRepository<PagamentoEntity, UUID> {
    Optional<PagamentoEntity> findByOrdemServicoId(Long ordemServicoId);
}
