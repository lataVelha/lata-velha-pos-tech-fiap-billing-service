package br.com.lata.velha.billing.application.use_cases;

import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway;
import br.com.lata.velha.billing.domain.entities.Pagamento;
import br.com.lata.velha.billing.domain.exceptions.PagamentoJaAprovadoException;
import br.com.lata.velha.billing.domain.repositories.PagamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class CriarPagamentoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CriarPagamentoUseCase.class);

    private final PagamentoRepository repository;
    private final MercadoPagoGateway mercadoPago;

    public CriarPagamentoUseCase(PagamentoRepository repository, MercadoPagoGateway mercadoPago) {
        this.repository = repository;
        this.mercadoPago = mercadoPago;
    }

    /**
     * Idempotente por ordem de serviço: se já existe um pagamento para a OS ele é
     * reaproveitado (e, se a criação da preferência tinha falhado antes, ela é
     * refeita). Só falha se a OS já estiver paga.
     */
    public Resultado execute(Long ordemServicoId, BigDecimal valor, String descricao, String emailPagador) {
        var existente = repository.buscarPorOrdemServicoId(ordemServicoId);
        if (existente.isPresent()) {
            var pagamento = existente.get();
            if (pagamento.isAprovado())
                throw new PagamentoJaAprovadoException(ordemServicoId);
            logger.info("Pagamento já existente para a OS - ordemServicoId={}, pagamentoId={}", ordemServicoId, pagamento.getId());
            return new Resultado(garantirCheckout(pagamento), false);
        }

        var pagamento = repository.salvar(Pagamento.create(ordemServicoId, valor, descricao, emailPagador));
        logger.info("Pagamento criado - ordemServicoId={}, pagamentoId={}, valor={}", ordemServicoId, pagamento.getId(), valor);
        return new Resultado(garantirCheckout(pagamento), true);
    }

    private Pagamento garantirCheckout(Pagamento pagamento) {
        if (pagamento.possuiCheckout())
            return pagamento;

        var preferencia = mercadoPago.criarPreferencia(pagamento);
        pagamento.registrarPreferencia(preferencia.id(), preferencia.checkoutUrl());
        logger.info("Preferência criada no Mercado Pago - pagamentoId={}, preferenciaId={}", pagamento.getId(), preferencia.id());
        return repository.salvar(pagamento);
    }

    public record Resultado(Pagamento pagamento, boolean criado) {
    }
}
