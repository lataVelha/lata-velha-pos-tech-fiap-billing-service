package br.com.lata.velha.billing.application.use_cases;

import br.com.lata.velha.shared.domain.events.CobrancaEmitidaEvent;
import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.exceptions.OrcamentoNotFoundException;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import br.com.lata.velha.shared.application.gateways.EventPublisher;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

public class EmitirCobrancaUseCase {

    private static final Logger logger = LoggerFactory.getLogger(EmitirCobrancaUseCase.class);

    private record TimelineStep(String status, String titulo, String descricao, int numero) {}

    private final OrcamentoRepository orcamentoRepository;
    private final CriarPagamentoUseCase criarPagamentoUseCase;
    private final EventPublisher eventPublisher;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String emailRemetente;

    public EmitirCobrancaUseCase(OrcamentoRepository orcamentoRepository,
                                 CriarPagamentoUseCase criarPagamentoUseCase,
                                 EventPublisher eventPublisher,
                                 JavaMailSender mailSender,
                                 TemplateEngine templateEngine,
                                 String emailRemetente) {
        this.orcamentoRepository = orcamentoRepository;
        this.criarPagamentoUseCase = criarPagamentoUseCase;
        this.eventPublisher = eventPublisher;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailRemetente = emailRemetente;
    }

    public void execute(Long ordemServicoId) {
        var orcamento = orcamentoRepository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new OrcamentoNotFoundException(ordemServicoId));

        var descricao = "Cobrança OS #" + ordemServicoId;
        var resultado = criarPagamentoUseCase.execute(ordemServicoId, orcamento.getValorTotal(), descricao, orcamento.getEmailCliente());

        eventPublisher.publish(new CobrancaEmitidaEvent(ordemServicoId));
        logger.info("Cobrança emitida - osId={}, valor={}", ordemServicoId, orcamento.getValorTotal());

        enviarEmailCobranca(orcamento, resultado.pagamento().getCheckoutUrl());
    }

    private void enviarEmailCobranca(Orcamento orcamento, String checkoutUrl) {
        if (orcamento.getEmailCliente() == null || orcamento.getEmailCliente().isBlank()) {
            logger.warn("Email do cliente não disponível, não foi possível enviar cobrança - osId={}", orcamento.getOrdemServicoId());
            return;
        }
        try {
            var timeline = List.of(
                    new TimelineStep("CONCLUIDO", "Diagnóstico concluído", "Problemas identificados", 1),
                    new TimelineStep("CONCLUIDO", "Orçamento aprovado", "Serviços autorizados", 2),
                    new TimelineStep("CONCLUIDO", "Execução concluída", "Serviços realizados", 3),
                    new TimelineStep("ATUAL", "Pagamento pendente", "Realize o pagamento para retirar o veículo", 4)
            );

            var ctx = new Context();
            ctx.setVariables(Map.of(
                    "reprovada", false,
                    "subtitulo", "Cobrança dos Serviços",
                    "nome", "Cliente",
                    "mensagem", "Os serviços do seu veículo foram concluídos. Realize o pagamento para retirar o veículo.",
                    "osNumero", orcamento.getOrdemServicoId(),
                    "veiculo", "",
                    "valorTotal", orcamento.getValorTotal(),
                    "checkoutUrl", checkoutUrl,
                    "timeline", timeline
            ));

            String html = templateEngine.process("os-notificacao", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailRemetente);
            helper.setTo(orcamento.getEmailCliente());
            helper.setSubject("Pagamento da Ordem de Serviço #" + orcamento.getOrdemServicoId());
            helper.setText(html, true);

            mailSender.send(message);
            logger.info("Email de cobrança enviado - osId={}, email={}", orcamento.getOrdemServicoId(), orcamento.getEmailCliente());
        } catch (Exception e) {
            logger.warn("Falha ao enviar email de cobrança - osId={}", orcamento.getOrdemServicoId(), e);
        }
    }
}
