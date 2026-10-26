#!/usr/bin/env bash
#
# apply.sh — deploy (ou destroy) completo do billing-service na AWS:
#
#   (padrão)   [1/4] Testes → [2/4] ECR + Docker → [3/4] Deploy (RDS + NLB +
#              API Gateway + Kubernetes) → [4/4] Verificar
#   --destroy  remove tudo do billing (inclusive o RDS e os dados nele)
#
# Pré-requisito: o cluster EKS (lata-velha-eks, repo infra) — se não existir,
# este script sobe o bootstrap do infra sozinho. Banco, ECR,
# entrada pública e manifests são todos deste repo.
#
# Uso:
#   ./apply.sh                  — deploy com confirmação interativa
#   ./apply.sh --auto           — deploy sem confirmação
#   ./apply.sh --skip-tests     — pula os testes Maven
#   ./apply.sh --destroy        — remove tudo, com confirmação
#   ./apply.sh --destroy --auto — remove tudo, sem confirmação
#
# Pré-requisitos:
#   cp terraform.tfvars.example terraform.tfvars
#   # Preencha o access token do Mercado Pago

set -Eeuo pipefail

# --------------------------- saída no terminal ------------------------------
if [[ -t 1 ]]; then
  C_BLUE=$'\033[1;34m'; C_GREEN=$'\033[1;32m'; C_YELLOW=$'\033[1;33m'; C_RED=$'\033[1;31m'; C_DIM=$'\033[2m'; C_RESET=$'\033[0m'
else
  C_BLUE=''; C_GREEN=''; C_YELLOW=''; C_RED=''; C_DIM=''; C_RESET=''
fi

AUTO=""
SKIP_TESTS=false
DESTROY=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --auto)                   AUTO="-auto-approve" ;;
    --skip-tests|--skip-test) SKIP_TESTS=true ;;
    --destroy)                DESTROY=true ;;
    *)
      echo "${C_RED}Flag desconhecida: $1${C_RESET}"
      echo "Uso: ./apply.sh [--auto] [--skip-tests] [--destroy]"
      exit 1
      ;;
  esac
  shift
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
REGION="${AWS_DEFAULT_REGION:-us-east-1}"
CLUSTER_NAME="lata-velha-eks"

echo "${C_BLUE}════════════════════════════════════════════════════════════${C_RESET}"
if $DESTROY; then
  echo "${C_YELLOW}  BILLING — Destruindo RDS (e os dados nele), ECR, NLB, API Gateway e K8s${C_RESET}"
else
  echo "  BILLING — Deploy do billing-service"
fi
echo "${C_BLUE}════════════════════════════════════════════════════════════${C_RESET}"

# Garante o Docker de pé logo no início (o build da imagem é no meio do
# pipeline — melhor não descobrir isso depois de criar coisa na AWS). Se o
# daemon estiver parado, sobe o Colima ou, sem ele, o Docker Desktop (macOS).
ensure_docker() {
  docker info > /dev/null 2>&1 && return 0

  if command -v colima &>/dev/null; then
    echo "${C_YELLOW}Docker parado — subindo o Colima...${C_RESET}"
    colima start
  elif [[ "$(uname)" == "Darwin" ]] && open -Ra Docker 2>/dev/null; then
    echo "${C_YELLOW}Docker parado — abrindo o Docker Desktop...${C_RESET}"
    open -a Docker
  else
    echo "${C_RED}✗ Docker não está rodando e não achei Colima nem Docker Desktop pra subir.${C_RESET}"
    exit 1
  fi

  local i
  for ((i = 0; i < 60; i++)); do
    docker info > /dev/null 2>&1 && { echo "${C_GREEN}  ✓ Docker pronto.${C_RESET}"; return 0; }
    sleep 2
  done
  echo "${C_RED}✗ Docker não respondeu em 2 min — verifique o daemon e rode de novo.${C_RESET}"
  exit 1
}

if ! $DESTROY; then
  ensure_docker
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
BUCKET="lata-velha-tfstate-${ACCOUNT_ID}"

echo "${C_DIM}Bucket de estado: $BUCKET${C_RESET}"
if ! aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
  echo "${C_DIM}  Bucket não existe — criando...${C_RESET}"
  aws s3 mb "s3://$BUCKET" --region "$REGION"
  aws s3api put-bucket-versioning \
    --bucket "$BUCKET" \
    --versioning-configuration Status=Enabled
  echo "${C_GREEN}  ✓ Bucket criado com versionamento ativado.${C_RESET}"
fi

# Garante que o cluster EKS existe e está ACTIVE. Se não existir, sobe o
# bootstrap do repo infra (VPC + EKS + ECR) — só ele, o billing não precisa
# dos addons (ALB/API Gateway compartilhados). INFRA_APPLY sobrescreve o
# caminho; o padrão é o layout do mono repo (../infra ao lado do billing).
ensure_cluster() {
  local status
  status=$(aws eks describe-cluster --name "$CLUSTER_NAME" --region "$REGION" \
    --query 'cluster.status' --output text 2>/dev/null || echo "MISSING")

  case "$status" in
    ACTIVE)
      echo "${C_DIM}Cluster $CLUSTER_NAME: ACTIVE${C_RESET}"
      ;;
    CREATING|UPDATING)
      echo "${C_YELLOW}Cluster $CLUSTER_NAME está $status — aguardando ficar ACTIVE...${C_RESET}"
      aws eks wait cluster-active --name "$CLUSTER_NAME" --region "$REGION"
      ;;
    MISSING)
      local infra_apply="${INFRA_APPLY:-$PROJECT_ROOT/../infra/terraform/apply.sh}"
      if [[ ! -x "$infra_apply" ]]; then
        echo "${C_RED}✗ Cluster $CLUSTER_NAME não existe e o apply.sh do infra não foi encontrado em:${C_RESET}"
        echo "  $infra_apply"
        echo "  Rode 'git submodule update --init infra' no mono repo ou defina INFRA_APPLY=/caminho/infra/terraform/apply.sh"
        exit 1
      fi
      echo "${C_YELLOW}Cluster $CLUSTER_NAME não existe — subindo o bootstrap do infra (VPC + EKS + ECR, ~15 min)...${C_RESET}"
      ( cd "$(dirname "$infra_apply")" && "./$(basename "$infra_apply")" --bootstrap-only ${AUTO:+--auto} )
      aws eks wait cluster-active --name "$CLUSTER_NAME" --region "$REGION"
      ;;
    *)
      echo "${C_RED}✗ Cluster $CLUSTER_NAME está $status — não dá pra fazer deploy nele agora.${C_RESET}"
      exit 1
      ;;
  esac

  # O NLB registra o ASG do node group — ele também precisa estar pronto.
  aws eks wait nodegroup-active --cluster-name "$CLUSTER_NAME" --nodegroup-name default --region "$REGION"
}

if ! $DESTROY; then
  ensure_cluster
fi

# Dados do cluster EKS — via AWS CLI, sem ler o Terraform state de outro repo.
export TF_VAR_region="$REGION"
export TF_VAR_cluster_name="$CLUSTER_NAME"
export TF_VAR_cluster_endpoint=$(aws eks describe-cluster --name "$CLUSTER_NAME" --region "$REGION" --query 'cluster.endpoint' --output text)
export TF_VAR_cluster_ca_data=$(aws eks describe-cluster --name "$CLUSTER_NAME" --region "$REGION" --query 'cluster.certificateAuthority.data' --output text)

terraform -chdir="$SCRIPT_DIR" init -reconfigure \
  -backend-config="bucket=${BUCKET}" \
  -backend-config="region=${REGION}"

if $DESTROY; then
  echo ""
  echo "${C_BLUE}==> Destruindo recursos do billing...${C_RESET}"
  # docker_image é obrigatória, mas não importa pro destroy
  TF_VAR_docker_image="destroy" terraform -chdir="$SCRIPT_DIR" destroy $AUTO
  echo ""
  echo "${C_GREEN}✓ Billing destruído.${C_RESET}"
  exit 0
fi

# [1/4] Testes (H2 em memória — não precisa de Postgres)
if ! $SKIP_TESTS; then
  echo ""
  echo "${C_BLUE}==> [1/4] Testes${C_RESET}"
  (cd "$PROJECT_ROOT" && ./mvnw -B clean test)
  echo "${C_GREEN}✓ [1/4] Testes concluídos com sucesso.${C_RESET}"
else
  echo ""
  echo "${C_YELLOW}==> [1/4] Testes — pulando (--skip-tests)${C_RESET}"
fi

# [2/4] ECR + Docker — o repositório ECR é deste Terraform, então é criado
# antes (apply com -target) pra ter onde fazer o push da imagem.
echo ""
echo "${C_BLUE}==> [2/4] ECR + Docker${C_RESET} ${C_DIM}— build e push da imagem${C_RESET}"
TF_VAR_docker_image="pending" terraform -chdir="$SCRIPT_DIR" apply $AUTO \
  -target=aws_ecr_repository.billing \
  -target=aws_ecr_lifecycle_policy.billing
ECR_URL=$(terraform -chdir="$SCRIPT_DIR" output -raw ecr_repository_url)
GIT_SHA=$(git -C "$PROJECT_ROOT" rev-parse --short HEAD 2>/dev/null || echo "local")
# Com mudanças não commitadas, a tag só do commit repetiria a da imagem
# anterior — o Deployment não mudaria e os pods não seriam recriados.
if [[ -n "$(git -C "$PROJECT_ROOT" status --porcelain 2>/dev/null)" ]]; then
  GIT_SHA="${GIT_SHA}-dirty-$(date +%Y%m%d%H%M%S)"
fi
IMAGE="${ECR_URL}:${GIT_SHA}"

aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "${ECR_URL%%/*}" > /dev/null

docker build --platform linux/amd64 -t "$IMAGE" "$PROJECT_ROOT"
docker push "$IMAGE"
echo "${C_GREEN}✓ [2/4] Imagem publicada:${C_RESET} $IMAGE"

# [3/4] Deploy
echo ""
echo "${C_BLUE}==> [3/4] Deploy${C_RESET} ${C_DIM}— RDS + NLB + API Gateway + Deployment/Service/HPA/PDB${C_RESET}"
export TF_VAR_docker_image="$IMAGE"
terraform -chdir="$SCRIPT_DIR" apply $AUTO
echo "${C_GREEN}✓ [3/4] Deploy aplicado.${C_RESET}"

# [4/4] Verificar
echo ""
echo "${C_BLUE}==> [4/4] Verificar${C_RESET} ${C_DIM}— aguardando rollout dos pods...${C_RESET}"
aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER_NAME" > /dev/null 2>&1

if command -v kubectl &>/dev/null; then
  kubectl rollout status deployment/lata-velha-billing \
    -n lata-velha-billing \
    --timeout=5m
  echo "${C_GREEN}✓ [4/4] Rollout concluído com sucesso.${C_RESET}"
else
  echo "${C_YELLOW}  kubectl não encontrado — verifique manualmente:${C_RESET}"
  echo "  kubectl rollout status deployment/lata-velha-billing -n lata-velha-billing"
fi

echo ""
echo "${C_GREEN}✓ BILLING concluído.${C_RESET}"
echo "  Webhook (cadastre no painel do Mercado Pago): $(terraform -chdir="$SCRIPT_DIR" output -raw webhook_url)"
echo "  Validação do x-signature: $(terraform -chdir="$SCRIPT_DIR" output -raw webhook_signature_validation)"
