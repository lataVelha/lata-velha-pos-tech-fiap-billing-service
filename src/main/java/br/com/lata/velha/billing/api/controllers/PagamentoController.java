package br.com.lata.velha.billing.api.controllers;

import br.com.lata.velha.billing.api.dtos.CriarPagamentoRequest;
import br.com.lata.velha.billing.api.dtos.PagamentoResponse;
import br.com.lata.velha.billing.application.use_cases.BuscarPagamentoUseCase;
import br.com.lata.velha.billing.application.use_cases.CriarPagamentoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Cobrança de ordens de serviço via Mercado Pago (Checkout Pro)")
public class PagamentoController {

    private final CriarPagamentoUseCase criarPagamento;
    private final BuscarPagamentoUseCase buscarPagamento;

    @PostMapping
    @Operation(summary = "Cria (ou recupera) o pagamento de uma OS e devolve a URL do checkout do Mercado Pago")
    public ResponseEntity<PagamentoResponse> criar(@Valid @RequestBody CriarPagamentoRequest request) {
        var resultado = criarPagamento.execute(request.ordemServicoId(), request.valor(), request.descricao(), request.emailPagador());
        var body = PagamentoResponse.from(resultado.pagamento());
        if (!resultado.criado())
            return ResponseEntity.ok(body);
        return ResponseEntity.created(URI.create("/pagamentos/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um pagamento")
    @ResponseStatus(HttpStatus.OK)
    public PagamentoResponse buscar(@PathVariable UUID id) {
        return PagamentoResponse.from(buscarPagamento.porId(id));
    }

    @GetMapping("/ordem-servico/{ordemServicoId}")
    @Operation(summary = "Consulta o pagamento de uma ordem de serviço")
    public PagamentoResponse buscarPorOrdemServico(@PathVariable Long ordemServicoId) {
        return PagamentoResponse.from(buscarPagamento.porOrdemServico(ordemServicoId));
    }
}
