package br.com.lata.velha.billing.infrastructure;

import br.com.lata.velha.billing.infrastructure.inventory.InventoryServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryServiceClientTest {

    private static final String BASE_URL = "http://inventory.test";

    private MockRestServiceServer server;
    private InventoryServiceClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new InventoryServiceClient(builder.build());
    }

    @Test
    void retornaValorDaPeca() {
        server.expect(requestTo(BASE_URL + "/pecas/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":1,"nome":"Pastilha de freio","valor":89.90}
                        """, MediaType.APPLICATION_JSON));

        var valor = client.buscarValorPeca(1L);

        assertThat(valor).contains(new java.math.BigDecimal("89.90"));
        server.verify();
    }

    @Test
    void retornaVazioQuandoRespostaNaoTemCorpo() {
        server.expect(requestTo(BASE_URL + "/pecas/2"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThat(client.buscarValorPeca(2L)).isEmpty();
        server.verify();
    }

    @Test
    void retornaVazioQuandoPecaNaoTemValor() {
        server.expect(requestTo(BASE_URL + "/pecas/3"))
                .andRespond(withSuccess("""
                        {"id":3,"nome":"Peça sem preço"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.buscarValorPeca(3L)).isEmpty();
        server.verify();
    }

    @Test
    void retornaVazioQuandoInventoryFalha() {
        server.expect(requestTo(BASE_URL + "/pecas/4"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(client.buscarValorPeca(4L)).isEmpty();
        server.verify();
    }
}
