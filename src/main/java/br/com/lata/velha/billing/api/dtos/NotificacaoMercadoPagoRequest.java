package br.com.lata.velha.billing.api.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificacaoMercadoPagoRequest(String type, String action, Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String id) {
    }
}
