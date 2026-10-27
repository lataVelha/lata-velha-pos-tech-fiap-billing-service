package br.com.lata.velha.billing.api.controllers;

import br.com.lata.velha.billing.application.use_cases.AprovarOrcamentoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orcamentos")
@RequiredArgsConstructor
@Tag(name = "Orçamentos", description = "Aprovação de orçamentos de ordens de serviço")
public class OrcamentoController {

    private final AprovarOrcamentoUseCase aprovarOrcamento;

    @PostMapping("/{ordemServicoId}/aprovar")
    @Operation(summary = "Aprovação do orçamento pelo cliente")
    public ResponseEntity<Void> aprovar(@PathVariable Long ordemServicoId) {
        aprovarOrcamento.execute(ordemServicoId);
        return ResponseEntity.noContent().build();
    }
}
