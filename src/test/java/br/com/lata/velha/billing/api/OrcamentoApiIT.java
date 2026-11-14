package br.com.lata.velha.billing.api;

import br.com.lata.velha.billing.domain.entities.Orcamento;
import br.com.lata.velha.billing.infrastructure.persistence.OrcamentoEntity;
import br.com.lata.velha.billing.infrastructure.persistence.OrcamentoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrcamentoApiIT {

    @Autowired MockMvc mvc;
    @Autowired OrcamentoJpaRepository jpa;

    @BeforeEach
    void setUp() {
        jpa.deleteAll();
    }

    private void salvarOrcamentoPendente(long osId) {
        var orcamento = Orcamento.create(osId, new BigDecimal("800.00"), "cliente@teste.com", List.of());
        jpa.save(OrcamentoEntity.from(orcamento));
    }

    @Test
    @WithMockUser(roles = "USER")
    void aprovaOrcamentoPendente() throws Exception {
        salvarOrcamentoPendente(20L);
        mvc.perform(post("/orcamentos/20/aprovar"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void reprovaOrcamentoPendente() throws Exception {
        salvarOrcamentoPendente(21L);
        mvc.perform(post("/orcamentos/21/reprovar"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void retorna404SeOrcamentoNaoExiste() throws Exception {
        mvc.perform(post("/orcamentos/999/aprovar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retorna401SemAutenticacao() throws Exception {
        salvarOrcamentoPendente(22L);
        mvc.perform(post("/orcamentos/22/aprovar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MECANICO")
    void retorna403ComRoleInsuficiente() throws Exception {
        salvarOrcamentoPendente(23L);
        mvc.perform(post("/orcamentos/23/aprovar"))
                .andExpect(status().isForbidden());
    }
}
