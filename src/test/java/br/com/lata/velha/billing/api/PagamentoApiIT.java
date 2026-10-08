package br.com.lata.velha.billing.api;

import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway;
import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway.PagamentoMercadoPago;
import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway.PreferenciaCriada;
import br.com.lata.velha.billing.domain.exceptions.IntegracaoPagamentoException;
import br.com.lata.velha.billing.infrastructure.persistence.PagamentoJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PagamentoApiIT {

    private static final String SECRET = "segredo-de-teste";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PagamentoJpaRepository jpa;
    @MockBean MercadoPagoGateway mercadoPago;

    @BeforeEach
    void setUp() {
        jpa.deleteAll();
        when(mercadoPago.criarPreferencia(any())).thenReturn(new PreferenciaCriada("pref-1", "https://mp/checkout?pref_id=pref-1"));
    }

    private String criar(long osId) throws Exception {
        var json = mvc.perform(post("/pagamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ordemServicoId": %d, "valor": 320.00, "descricao": "Troca de pastilhas", "emailPagador": "cliente@teste.com"}
                                """.formatted(osId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://mp/checkout?pref_id=pref-1"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("id").asText();
    }

    private static String assinatura(String dataId, String requestId, String ts) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var manifesto = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        return "ts=" + ts + ",v1=" + HexFormat.of().formatHex(mac.doFinal(manifesto.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void fluxoCompletoCriarNotificarConsultar() throws Exception {
        var id = criar(7L);

        // Segunda chamada para a mesma OS é idempotente
        mvc.perform(post("/pagamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordemServicoId\":7,\"valor\":320.00,\"descricao\":\"Troca de pastilhas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        verify(mercadoPago, times(1)).criarPreferencia(any());

        when(mercadoPago.buscarPagamento(999L)).thenReturn(new PagamentoMercadoPago(999L, "approved", "accredited", id));

        mvc.perform(post("/pagamentos/webhook").param("data.id", "999").param("type", "payment")
                        .header("x-signature", assinatura("999", "req-1", "1700000000"))
                        .header("x-request-id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment\",\"action\":\"payment.updated\",\"data\":{\"id\":\"999\"}}"))
                .andExpect(status().isOk());

        mvc.perform(get("/pagamentos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.mercadoPagoPaymentId").value(999));

        mvc.perform(get("/pagamentos/ordem-servico/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        // OS já paga não aceita nova cobrança
        mvc.perform(post("/pagamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordemServicoId\":7,\"valor\":320.00,\"descricao\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void webhookComAssinaturaInvalidaRetorna401() throws Exception {
        mvc.perform(post("/pagamentos/webhook").param("data.id", "999").param("type", "payment")
                        .header("x-signature", "ts=1,v1=deadbeef")
                        .header("x-request-id", "req-1"))
                .andExpect(status().isUnauthorized());
        verify(mercadoPago, never()).buscarPagamento(any());
    }

    @Test
    void webhookDeOutroTopicoEhIgnorado() throws Exception {
        mvc.perform(post("/pagamentos/webhook").param("data.id", "abc").param("type", "merchant_order")
                        .header("x-signature", assinatura("abc", "req-2", "1"))
                        .header("x-request-id", "req-2"))
                .andExpect(status().isOk());
        verify(mercadoPago, never()).buscarPagamento(any());
    }

    @Test
    void falhaNoMercadoPagoNoWebhookRetorna5xxParaRetentativa() throws Exception {
        when(mercadoPago.buscarPagamento(1L)).thenThrow(new IntegracaoPagamentoException("fora do ar", null));
        mvc.perform(post("/pagamentos/webhook").param("data.id", "1").param("type", "payment")
                        .header("x-signature", assinatura("1", "req-3", "1"))
                        .header("x-request-id", "req-3"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void validacoesERecursoInexistente() throws Exception {
        mvc.perform(post("/pagamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordemServicoId\":1,\"valor\":-1,\"descricao\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/pagamentos/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/pagamentos/ordem-servico/12345"))
                .andExpect(status().isNotFound());
    }

    @Test
    void falhaAoCriarPreferenciaRetorna502ENovaTentativaReaproveitaPagamento() throws Exception {
        when(mercadoPago.criarPreferencia(any()))
                .thenThrow(new IntegracaoPagamentoException("fora do ar", null))
                .thenReturn(new PreferenciaCriada("pref-2", "https://mp/checkout?pref_id=pref-2"));

        var body = "{\"ordemServicoId\":8,\"valor\":10.00,\"descricao\":\"x\"}";
        mvc.perform(post("/pagamentos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadGateway());
        mvc.perform(post("/pagamentos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://mp/checkout?pref_id=pref-2"));
    }
}
