# O billing-service e autocontido: tem banco (RDS), ECR, entrada publica
# (API Gateway + NLB interno) e deploy no Kubernetes proprios. A unica coisa
# que ele precisa que exista antes e o cluster EKS — de onde saem VPC,
# subnets privadas, security group dos nodes e o ASG, consultados direto na
# AWS (sem terraform_remote_state de outro repo).

data "aws_eks_cluster" "this" {
  name = var.cluster_name
}

data "aws_eks_node_group" "this" {
  cluster_name    = var.cluster_name
  node_group_name = var.node_group_name
}

locals {
  name = "${var.project_name}-billing"

  vpc_id = data.aws_eks_cluster.this.vpc_config[0].vpc_id
  # O cluster e os nodes vivem nas subnets privadas (repo infra, bootstrap)
  private_subnet_ids = tolist(data.aws_eks_cluster.this.vpc_config[0].subnet_ids)
  # Node group gerenciado sem SG proprio no launch template usa o SG do cluster
  node_security_group_id = data.aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
  node_asg_name          = data.aws_eks_node_group.this.resources[0].autoscaling_groups[0].name
}

data "aws_vpc" "this" {
  id = local.vpc_id
}

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
