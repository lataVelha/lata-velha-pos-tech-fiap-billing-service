variable "region" {
  description = "Regiao da AWS"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefixo dos recursos"
  type        = string
  default     = "lata-velha"
}

variable "environment" {
  description = "Ambiente (dev, staging, prod)"
  type        = string
  default     = "dev"
}

# Conexao com o EKS — lidas via AWS CLI no apply.sh/pipeline e passadas como
# TF_VAR_, ja que provider config nao usa data sources para a propria
# inicializacao. VPC, subnets e security group do cluster saem do proprio
# cluster (data "aws_eks_cluster"), sem ler o state de outro repo.
variable "cluster_name" {
  description = "Nome do EKS cluster onde o billing roda"
  type        = string
  default     = "lata-velha-eks"
}

variable "cluster_endpoint" {
  description = "Endpoint do EKS cluster"
  type        = string
}

variable "cluster_ca_data" {
  description = "CA certificate do EKS cluster (base64)"
  type        = string
  sensitive   = true
}

variable "node_group_name" {
  description = "Node group do EKS cujo ASG e registrado no NLB"
  type        = string
  default     = "default"
}

variable "node_port" {
  description = "NodePort do Service do billing (destino do NLB). O app usa 30080"
  type        = number
  default     = 30081
}

variable "docker_image" {
  description = "Imagem ECR do billing (definida pelo apply.sh/pipeline apos o build/push)"
  type        = string
}

variable "db_name" {
  description = "Nome do banco do billing"
  type        = string
  default     = "lata_velha_billing"
}

variable "db_username" {
  description = "Usuario do banco do billing"
  type        = string
  default     = "billing_user"
  sensitive   = true
}

variable "rds_instance_class" {
  description = "Classe da instancia RDS"
  type        = string
  default     = "db.t3.micro"
}

variable "mercadopago_access_token" {
  description = "Access token da aplicacao no Mercado Pago (painel -> Credenciais)"
  type        = string
  sensitive   = true

  validation {
    condition     = length(trimspace(var.mercadopago_access_token)) > 0
    error_message = "mercadopago_access_token nao pode ser vazio — sem ele o POST /pagamentos falha ao criar a preferencia."
  }
}

variable "mercadopago_webhook_secret" {
  description = "Chave secreta do painel Webhooks do Mercado Pago. Vazio = validacao do x-signature desligada"
  type        = string
  sensitive   = true
  # Vazio so no 1o deploy: a chave so e gerada no painel depois de cadastrar a
  # URL do webhook (output webhook_url). Preencha e rode o apply de novo.
  default = ""
}

variable "mercadopago_back_url_success" {
  description = "Para onde o comprador volta apos pagamento aprovado (opcional)"
  type        = string
  default     = ""
}

variable "mercadopago_back_url_failure" {
  description = "Para onde o comprador volta apos pagamento recusado (opcional)"
  type        = string
  default     = ""
}

variable "mercadopago_back_url_pending" {
  description = "Para onde o comprador volta com pagamento pendente (opcional)"
  type        = string
  default     = ""
}

variable "log_retention_days" {
  description = "Retencao dos access logs do API Gateway"
  type        = number
  default     = 7
}
