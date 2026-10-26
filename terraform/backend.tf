terraform {
  backend "s3" {
    key = "lata-velha/billing-service/terraform.tfstate"
    # Lock de estado nativo do S3 (sem DynamoDB)
    use_lockfile = true
  }
}
