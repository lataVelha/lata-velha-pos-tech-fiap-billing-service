locals {
  k8s_dir     = "${path.module}/../k8s"
  webhook_url = "${aws_apigatewayv2_api.billing.api_endpoint}/pagamentos/webhook"

  configmap_yaml = templatefile("${local.k8s_dir}/configmap.yaml", {
    notification_url = local.webhook_url
    back_url_success = var.mercadopago_back_url_success
    back_url_failure = var.mercadopago_back_url_failure
    back_url_pending = var.mercadopago_back_url_pending
  })

  secret_yaml = templatefile("${local.k8s_dir}/secret.yaml", {
    db_url_b64            = base64encode("jdbc:postgresql://${aws_db_instance.billing.endpoint}/${aws_db_instance.billing.db_name}")
    db_username_b64       = base64encode(var.db_username)
    db_password_b64       = base64encode(random_password.db.result)
    mp_access_token_b64   = base64encode(var.mercadopago_access_token)
    mp_webhook_secret_b64 = base64encode(var.mercadopago_webhook_secret)
  })
}

resource "kubectl_manifest" "namespace" {
  yaml_body = file("${local.k8s_dir}/namespace.yaml")
}

resource "kubectl_manifest" "configmap" {
  yaml_body  = local.configmap_yaml
  depends_on = [kubectl_manifest.namespace]
}

resource "kubectl_manifest" "secret" {
  yaml_body        = local.secret_yaml
  sensitive_fields = ["data"]
  depends_on       = [kubectl_manifest.namespace]
}

resource "kubectl_manifest" "deployment" {
  yaml_body = templatefile("${local.k8s_dir}/deployment.yaml", {
    docker_image    = var.docker_image
    config_checksum = sha256("${local.configmap_yaml}${local.secret_yaml}")
  })
  depends_on = [
    kubectl_manifest.configmap,
    kubectl_manifest.secret,
  ]
}

resource "kubectl_manifest" "service" {
  yaml_body = templatefile("${local.k8s_dir}/service.yaml", {
    node_port = var.node_port
  })
  depends_on = [kubectl_manifest.deployment]
}

resource "kubectl_manifest" "hpa" {
  yaml_body  = file("${local.k8s_dir}/hpa.yaml")
  depends_on = [kubectl_manifest.deployment]
}

resource "kubectl_manifest" "pdb" {
  yaml_body  = file("${local.k8s_dir}/pdb.yaml")
  depends_on = [kubectl_manifest.deployment]
}
