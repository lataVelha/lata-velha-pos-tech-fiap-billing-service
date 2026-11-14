# O billing-service usa o ALB compartilhado (infra/addons) como entrada publica.
# A unica coisa que precisa existir antes e o cluster EKS — VPC, subnets e
# security group dos nodes sao consultados direto na AWS.

data "aws_eks_cluster" "this" {
  name = var.cluster_name
}

locals {
  name = "${var.project_name}-billing"

  vpc_id = data.aws_eks_cluster.this.vpc_config[0].vpc_id
  # O cluster e os nodes vivem nas subnets privadas (repo infra, bootstrap)
  private_subnet_ids = tolist(data.aws_eks_cluster.this.vpc_config[0].subnet_ids)
  # Node group gerenciado sem SG proprio no launch template usa o SG do cluster
  node_security_group_id = data.aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
}

# ALB compartilhado — le o endpoint do API Gateway publico para montar o webhook_url.
data "terraform_remote_state" "addons" {
  backend = "s3"
  config = {
    bucket = "lata-velha-tfstate-${data.aws_caller_identity.current.account_id}"
    key    = "lata-velha/infra-addons/terraform.tfstate"
    region = var.region
  }
}

# Barramento SNS/SQS (Epic 13) — topic ARN e a fila do billing.
data "terraform_remote_state" "messaging" {
  backend = "s3"
  config = {
    bucket = "lata-velha-tfstate-${data.aws_caller_identity.current.account_id}"
    key    = var.messaging_state_key
    region = var.region
  }
}

data "aws_caller_identity" "current" {}

resource "aws_ecr_repository" "billing" {
  name                 = local.name
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "billing" {
  repository = aws_ecr_repository.billing.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Manter so as 10 imagens mais recentes"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = { type = "expire" }
      }
    ]
  })
}
