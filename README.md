# Billing Service — Lata Velha

Microsserviço de cobrança das ordens de serviço do **Lata Velha**, integrado ao
**Mercado Pago (Checkout Pro)**. Recebe o valor de uma OS, cria a preferência de
pagamento no Mercado Pago, devolve a URL do checkout (cartão, PIX, boleto) e
acompanha o status do pagamento pelos **Webhooks** do Mercado Pago.

Mesma stack e organização do `app`: Java 21, Spring Boot 3.2.5, PostgreSQL + Flyway,
camadas `domain` / `application` / `infrastructure` / `api`. Banco próprio
(`lata_velha_billing`) — o serviço não acessa o banco do `app`.

## Fluxo

```
cliente/app ── POST /pagamentos ──► Billing Service ── POST /checkout/preferences ──► Mercado Pago
            ◄── checkoutUrl ───────                  ◄── init_point ──────────────
cliente ── paga no checkoutUrl ─────────────────────────────────────────────────────► Mercado Pago
Mercado Pago ── POST /pagamentos/webhook (x-signature) ──► Billing Service
                                                           ── GET /v1/payments/{id} ──► Mercado Pago
                                                           ◄── status, external_reference
```

- `external_reference` da preferência = id do pagamento no Billing Service; é por ele
  que a notificação é ligada ao pagamento.
- O webhook **nunca confia no corpo da notificação**: sempre consulta o payment na API
  do Mercado Pago. Notificações repetidas ou fora de ordem não corrompem o estado.
- Depois de `APROVADO`, o pagamento só pode ir para `REEMBOLSADO` (uma notificação
  atrasada de uma tentativa recusada não "desaprova" a OS).

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/pagamentos` | Cria a cobrança de uma OS (`201`) — idempotente: se a OS já tem cobrança, devolve a mesma (`200`); se já foi paga, `409` |
| `GET` | `/pagamentos/{id}` | Consulta um pagamento |
| `GET` | `/pagamentos/ordem-servico/{ordemServicoId}` | Consulta o pagamento de uma OS |
| `POST` | `/pagamentos/webhook` | Notificação do Mercado Pago (pública, validada por `x-signature`) |

Swagger: `http://localhost:8081/swagger-ui.html`

```bash
curl -X POST http://localhost:8081/pagamentos \
  -H 'Content-Type: application/json' \
  -d '{"ordemServicoId": 42, "valor": 320.00, "descricao": "OS 42 - troca de pastilhas", "emailPagador": "cliente@teste.com"}'
```

Status possíveis: `PENDENTE`, `EM_PROCESSAMENTO`, `APROVADO`, `REJEITADO`,
`CANCELADO`, `REEMBOLSADO`.

## Configuração

| Variável | Descrição |
| --- | --- |
| `MERCADOPAGO_ACCESS_TOKEN` | Access token da aplicação (painel do Mercado Pago → Credenciais). Use o de teste localmente |
| `MERCADOPAGO_WEBHOOK_SECRET` | Chave secreta do painel → Webhooks. **Obrigatória em produção** — vazia desliga a validação da assinatura |
| `MERCADOPAGO_NOTIFICATION_URL` | URL pública do webhook (ex.: `https://<api-gateway>/pagamentos/webhook`) |
| `MERCADOPAGO_BACK_URL_SUCCESS` / `_FAILURE` / `_PENDING` | Para onde o comprador volta depois do checkout (opcionais) |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | PostgreSQL do serviço |

## Rodando

```bash
cp .env.example .env   # preencha MERCADOPAGO_ACCESS_TOKEN=TEST-... (não versionado)
docker compose up --build
```

`BILLING_PORT` no `.env` muda a porta exposta no host (padrão `8081`).

Para receber webhooks localmente, exponha a porta 8081 (ex.: `ngrok http 8081`) e use
a URL gerada + `/pagamentos/webhook` em `MERCADOPAGO_NOTIFICATION_URL`.

## Testes

```bash
./mvnw test
```

Testes de domínio, de casos de uso, do cliente HTTP (`MockRestServiceServer`), da
validação de assinatura e de integração ponta a ponta (`PagamentoApiIT`, H2).
