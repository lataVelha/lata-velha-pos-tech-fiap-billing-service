# Entrada publica do billing: API Gateway (HTTPS) -> VPC Link -> NLB interno
# -> NodePort nos nodes do EKS. So o webhook do Mercado Pago e publicado —
# POST/GET /pagamentos ficam acessiveis apenas dentro do cluster
# (http://lata-velha-billing.lata-velha-billing.svc.cluster.local), ja que
# este gateway nao tem a authorizer do repo lambda.

resource "aws_lb" "billing" {
  name               = "${local.name}-nlb"
  internal           = true
  load_balancer_type = "network"
  subnets            = local.private_subnet_ids
}

resource "aws_lb_target_group" "billing" {
  name        = "${local.name}-tg"
  port        = var.node_port
  protocol    = "TCP"
  vpc_id      = local.vpc_id
  target_type = "instance"
  # <= terminationGracePeriodSeconds (60s) do Deployment
  deregistration_delay = 60

  health_check {
    protocol            = "HTTP"
    path                = "/actuator/health/readiness"
    port                = tostring(var.node_port)
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_lb_listener" "billing" {
  load_balancer_arn = aws_lb.billing.arn
  port              = 80
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.billing.arn
  }
}

# Registra os nodes no target group via ASG — o autoscaler adiciona/remove
# nodes e o NLB acompanha sozinho.
resource "aws_autoscaling_attachment" "billing" {
  autoscaling_group_name = local.node_asg_name
  lb_target_group_arn    = aws_lb_target_group.billing.arn
}

# NLB sem security group preserva o IP de origem (ENIs do VPC Link e health
# checks do proprio NLB, todos dentro da VPC) — libera o NodePort pra VPC.
resource "aws_security_group_rule" "vpc_to_nodes" {
  type              = "ingress"
  from_port         = var.node_port
  to_port           = var.node_port
  protocol          = "tcp"
  security_group_id = local.node_security_group_id
  cidr_blocks       = [data.aws_vpc.this.cidr_block]
  description       = "NLB/VPC Link do billing para os nodes (NodePort ${var.node_port})"
}

resource "aws_security_group" "vpc_link" {
  name        = "${local.name}-vpc-link-sg"
  description = "VPC Link do API Gateway do billing"
  vpc_id      = local.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_apigatewayv2_api" "billing" {
  name          = "${local.name}-api"
  protocol_type = "HTTP"
  description   = "Entrada publica do billing-service (webhook do Mercado Pago)"
}

resource "aws_apigatewayv2_vpc_link" "billing" {
  name               = "${local.name}-vpc-link"
  security_group_ids = [aws_security_group.vpc_link.id]
  subnet_ids         = local.private_subnet_ids
}

# payload_format_version precisa ser "1.0" — integracoes privadas (VPC_LINK)
# nao suportam 2.0. HTTP_PROXY repassa a query string (data.id, type), que
# entra no manifesto do x-signature.
resource "aws_apigatewayv2_integration" "billing" {
  api_id                 = aws_apigatewayv2_api.billing.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = aws_lb_listener.billing.arn
  integration_method     = "ANY"
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.billing.id
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "webhook" {
  api_id    = aws_apigatewayv2_api.billing.id
  route_key = "POST /pagamentos/webhook"
  target    = "integrations/${aws_apigatewayv2_integration.billing.id}"
}

resource "aws_cloudwatch_log_group" "api" {
  name              = "/aws/apigateway/${local.name}-api"
  retention_in_days = var.log_retention_days
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.billing.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api.arn
    format = jsonencode({
      requestId        = "$context.requestId"
      ip               = "$context.identity.sourceIp"
      requestTime      = "$context.requestTime"
      httpMethod       = "$context.httpMethod"
      routeKey         = "$context.routeKey"
      status           = "$context.status"
      responseLength   = "$context.responseLength"
      integrationError = "$context.integrationErrorMessage"
    })
  }
}
