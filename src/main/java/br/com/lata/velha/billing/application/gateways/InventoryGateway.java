package br.com.lata.velha.billing.application.gateways;

import java.math.BigDecimal;
import java.util.Optional;

public interface InventoryGateway {
    Optional<BigDecimal> buscarValorPeca(Long pecaId);
}
