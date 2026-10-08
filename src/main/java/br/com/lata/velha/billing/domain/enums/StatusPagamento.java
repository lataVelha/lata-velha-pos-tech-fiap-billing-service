package br.com.lata.velha.billing.domain.enums;

public enum StatusPagamento {
    PENDENTE,
    EM_PROCESSAMENTO,
    APROVADO,
    REJEITADO,
    CANCELADO,
    REEMBOLSADO;

    /**
     * Converte o campo "status" de um payment do Mercado Pago.
     * Ver https://www.mercadopago.com.br/developers/pt/reference/payments/_payments_id/get
     */
    public static StatusPagamento fromMercadoPago(String status) {
        if (status == null)
            throw new IllegalArgumentException("Status do Mercado Pago não informado");

        return switch (status) {
            case "pending" -> PENDENTE;
            case "authorized", "in_process", "in_mediation" -> EM_PROCESSAMENTO;
            case "approved" -> APROVADO;
            case "rejected" -> REJEITADO;
            case "cancelled" -> CANCELADO;
            case "refunded", "charged_back" -> REEMBOLSADO;
            default -> throw new IllegalArgumentException("Status do Mercado Pago desconhecido: " + status);
        };
    }
}
