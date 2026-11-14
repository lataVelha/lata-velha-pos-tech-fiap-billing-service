package br.com.lata.velha.billing.api;

import br.com.lata.velha.billing.application.gateways.MercadoPagoGateway;
import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.domain.repositories.OrcamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrcamentoApiIT {

    @Autowired MockMvc mvc;
    @Autowired OrcamentoRepository orcamentoRepository;
    @MockBean MercadoPagoGateway mercadoPago;

    @BeforeEach
    void setUp() {
        // limpa orcamentos entre testes via repositório
    }

    @Test
    void reprovarOrcamentoPendenteRetorna204() throws Exception {
        var orcamento = Orcamento.create(50L, new BigDecimal("800.00"), "cliente@teste.com", List.of());
        orcamentoRepository.salvar(orcamento);

        mvc.perform(post("/orcamentos/50/reprovar"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reprovarOrcamentoInexistenteRetorna404() throws Exception {
        mvc.perform(post("/orcamentos/9999/reprovar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aprovarOrcamentoPendenteRetorna204() throws Exception {
        var orcamento = Orcamento.create(51L, new BigDecimal("600.00"), "cliente@teste.com", List.of());
        orcamentoRepository.salvar(orcamento);

        mvc.perform(post("/orcamentos/51/aprovar"))
                .andExpect(status().isNoContent());
    }

    @Test
    void aprovarOrcamentoInexistenteRetorna404() throws Exception {
        mvc.perform(post("/orcamentos/9998/aprovar"))
                .andExpect(status().isNotFound());
    }
}
