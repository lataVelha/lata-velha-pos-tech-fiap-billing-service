output "webhook_url" {
  description = "URL para cadastrar no painel do Mercado Pago (Webhooks -> Modo producao)"
  value       = local.webhook_url
}

output "ecr_repository_url" {
  value = aws_ecr_repository.billing.repository_url
}

output "rds_endpoint" {
  description = "Endpoint do RDS do billing (host:porta)"
  value       = aws_db_instance.billing.endpoint
}

output "internal_url" {
  description = "URL do billing dentro do cluster (para o app/os-service chamarem POST /pagamentos)"
  value       = "http://lata-velha-billing.lata-velha-billing.svc.cluster.local"
}

output "webhook_signature_validation" {
  value = nonsensitive(var.mercadopago_webhook_secret == "") ? "DESLIGADA — cadastre webhook_url no painel, copie a chave secreta para mercadopago_webhook_secret e rode o apply de novo" : "ligada"
}
