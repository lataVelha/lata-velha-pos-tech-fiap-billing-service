package br.com.lata.velha.billing.api.dtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CriarPagamentoRequest(
        @NotNull @Positive Long ordemServicoId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal valor,
        @NotBlank @Size(max = 255) String descricao,
        @Email @Size(max = 255) String emailPagador) {
}
