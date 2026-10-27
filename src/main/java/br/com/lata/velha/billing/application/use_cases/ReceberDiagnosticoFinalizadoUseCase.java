package br.com.lata.velha.billing.application.use_cases;

import br.com.lata.velha.billing.application.gateways.InventoryGateway;
import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.events.DiagnosticoFinalizadoEvent;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

public class ReceberDiagnosticoFinalizadoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ReceberDiagnosticoFinalizadoUseCase.class);

    private final OrcamentoRepository orcamentoRepository;
    private final InventoryGateway inventoryGateway;
    private final JavaMailSender mailSender;
    private final String emailRemetente;

    public ReceberDiagnosticoFinalizadoUseCase(OrcamentoRepository orcamentoRepository,
                                               InventoryGateway inventoryGateway,
                                               JavaMailSender mailSender,
                                               String emailRemetente) {
        this.orcamentoRepository = orcamentoRepository;
        this.inventoryGateway = inventoryGateway;
        this.mailSender = mailSender;
        this.emailRemetente = emailRemetente;
    }

    public void execute(DiagnosticoFinalizadoEvent event) {
        var existente = orcamentoRepository.buscarPorOrdemServicoId(event.getOsId());
        if (existente.isPresent()) {
            logger.info("Orçamento já existente para OS, ignorando - osId={}", event.getOsId());
            return;
        }

        var valorTotal = calcularTotal(event);
        var orcamento = orcamentoRepository.salvar(Orcamento.create(event.getOsId(), valorTotal, null));

        logger.info("Orçamento criado - osId={}, orcamentoId={}, valorTotal={}", event.getOsId(), orcamento.getId(), valorTotal);
        enviarEmailOrcamento(orcamento);
    }

    private BigDecimal calcularTotal(DiagnosticoFinalizadoEvent event) {
        var total = BigDecimal.ZERO;
        for (var item : event.getItens()) {
            total = total.add(item.valorMaoDeObra() != null ? item.valorMaoDeObra() : BigDecimal.ZERO);
            for (var peca : item.pecas()) {
                var valorPeca = inventoryGateway.buscarValorPeca(peca.pecaId()).orElse(BigDecimal.ZERO);
                total = total.add(valorPeca.multiply(BigDecimal.valueOf(peca.quantidade())));
            }
        }
        return total;
    }

    private void enviarEmailOrcamento(Orcamento orcamento) {
        if (orcamento.getEmailCliente() == null || orcamento.getEmailCliente().isBlank()) {
            logger.warn("Email do cliente não disponível, não foi possível enviar orçamento - osId={}", orcamento.getOrdemServicoId());
            return;
        }
        try {
            var msg = new SimpleMailMessage();
            msg.setFrom(emailRemetente);
            msg.setTo(orcamento.getEmailCliente());
            msg.setSubject("Orçamento da sua Ordem de Serviço #" + orcamento.getOrdemServicoId());
            msg.setText(String.format(
                    "Olá! O diagnóstico do seu veículo foi concluído.%n%n" +
                    "Valor total do orçamento: R$ %.2f%n%n" +
                    "Para aprovar, acesse o link ou entre em contato conosco.%n%n" +
                    "Lata Velha Oficina",
                    orcamento.getValorTotal()));
            mailSender.send(msg);
            logger.info("Email de orçamento enviado - osId={}, email={}", orcamento.getOrdemServicoId(), orcamento.getEmailCliente());
        } catch (Exception e) {
            logger.warn("Falha ao enviar email de orçamento - osId={}", orcamento.getOrdemServicoId(), e);
        }
    }
}
