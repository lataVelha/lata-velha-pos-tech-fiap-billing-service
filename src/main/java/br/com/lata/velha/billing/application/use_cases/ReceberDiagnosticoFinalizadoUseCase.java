package br.com.lata.velha.billing.application.use_cases;

import br.com.lata.velha.billing.application.gateways.InventoryGateway;
import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.entities.OrcamentoItem;
import br.com.lata.velha.billing.domain.events.DiagnosticoFinalizadoEvent;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ReceberDiagnosticoFinalizadoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ReceberDiagnosticoFinalizadoUseCase.class);

    private record TimelineStep(String status, String titulo, String descricao, int numero) {}

    private final OrcamentoRepository orcamentoRepository;
    private final InventoryGateway inventoryGateway;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String emailRemetente;

    public ReceberDiagnosticoFinalizadoUseCase(OrcamentoRepository orcamentoRepository,
                                               InventoryGateway inventoryGateway,
                                               JavaMailSender mailSender,
                                               TemplateEngine templateEngine,
                                               String emailRemetente) {
        this.orcamentoRepository = orcamentoRepository;
        this.inventoryGateway = inventoryGateway;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailRemetente = emailRemetente;
    }

    public void execute(DiagnosticoFinalizadoEvent event) {
        var existente = orcamentoRepository.buscarPorOrdemServicoId(event.getOsId());
        if (existente.isPresent()) {
            logger.info("Orçamento já existente para OS, ignorando - osId={}", event.getOsId());
            return;
        }

        var valorTotal = calcularTotal(event);
        var itens = extrairItens(event);
        var orcamento = orcamentoRepository.salvar(
                Orcamento.create(event.getOsId(), valorTotal, null, itens));

        logger.info("Orçamento criado - osId={}, orcamentoId={}, valorTotal={}, itens={}",
                event.getOsId(), orcamento.getId(), valorTotal, itens.size());
        enviarEmailOrcamento(orcamento);
    }

    private List<OrcamentoItem> extrairItens(DiagnosticoFinalizadoEvent event) {
        if (event.getItens() == null) {
            return List.of();
        }
        return event.getItens().stream()
                .flatMap(item -> item.pecas() == null ? java.util.stream.Stream.<OrcamentoItem>empty()
                        : item.pecas().stream()
                        .map(peca -> new OrcamentoItem(
                                item.execucaoServicoId(), peca.pecaId(), peca.quantidade())))
                .toList();
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
            var timeline = List.of(
                    new TimelineStep("CONCLUIDO", "Diagnóstico concluído", "Problemas identificados", 1),
                    new TimelineStep("ATUAL", "Aguardando aprovação", "Orçamento enviado para análise", 2),
                    new TimelineStep("PENDENTE", "Execução dos serviços", "Após aprovação do orçamento", 3),
                    new TimelineStep("PENDENTE", "Pagamento", "Finalização da ordem de serviço", 4)
            );

            var ctx = new Context();
            ctx.setVariables(Map.of(
                    "reprovada", false,
                    "subtitulo", "Orçamento do Diagnóstico",
                    "nome", "Cliente",
                    "mensagem", "O diagnóstico do seu veículo foi concluído. Confira o orçamento abaixo e entre em contato para aprovar os serviços.",
                    "osNumero", orcamento.getOrdemServicoId(),
                    "veiculo", "",
                    "valorTotal", orcamento.getValorTotal(),
                    "timeline", timeline
            ));

            String html = templateEngine.process("os-notificacao", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailRemetente);
            helper.setTo(orcamento.getEmailCliente());
            helper.setSubject("Orçamento da sua Ordem de Serviço #" + orcamento.getOrdemServicoId());
            helper.setText(html, true);

            mailSender.send(message);
            logger.info("Email de orçamento enviado - osId={}, email={}", orcamento.getOrdemServicoId(), orcamento.getEmailCliente());
        } catch (Exception e) {
            logger.warn("Falha ao enviar email de orçamento - osId={}", orcamento.getOrdemServicoId(), e);
        }
    }
}
