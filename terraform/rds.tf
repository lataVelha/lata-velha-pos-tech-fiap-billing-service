# Banco proprio do billing — nao depende do RDS do repo infra-db.

resource "random_password" "db" {
  length  = 24
  special = false
}

resource "aws_db_subnet_group" "billing" {
  name       = "${local.name}-subnet-group"
  subnet_ids = local.private_subnet_ids

  tags = {
    Name = "${local.name}-subnet-group"
  }
}

resource "aws_security_group" "rds" {
  name        = "${local.name}-rds-sg"
  description = "Acesso ao RDS do billing a partir da VPC"
  vpc_id      = local.vpc_id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.this.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name}-rds-sg"
  }
}

resource "aws_db_instance" "billing" {
  identifier              = "${local.name}-postgres"
  engine                  = "postgres"
  engine_version          = "15"
  instance_class          = var.rds_instance_class
  allocated_storage       = 20
  db_name                 = var.db_name
  username                = var.db_username
  password                = random_password.db.result
  db_subnet_group_name    = aws_db_subnet_group.billing.name
  vpc_security_group_ids  = [aws_security_group.rds.id]
  skip_final_snapshot     = true
  backup_retention_period = 0
  publicly_accessible     = false
  multi_az                = false
  storage_encrypted       = true

  tags = {
    Name = "${local.name}-postgres"
  }
}
