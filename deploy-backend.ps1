# Variáveis
$region = "sa-east-1"
$repositoryUri = "775525057355.dkr.ecr.sa-east-1.amazonaws.com/mcloud-backend"
$imageTag = "latest"
$jarPath = "target/auth0-authenticator.jar"

# Verifica se o JAR existe
if (-Not (Test-Path $jarPath)) {
    Write-Host "Erro: JAR não encontrado em $jarPath"
    exit 1
}

# Autenticação no ECR
aws ecr get-login-password --region $region | docker login --username AWS --password-stdin $repositoryUri

# Build da imagem com nome completo
docker build -t "${repositoryUri}:${imageTag}" .

# Push da imagem para o ECR
docker push "${repositoryUri}:${imageTag}"
