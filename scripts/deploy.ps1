#Requires -Version 7.0
#Requires -Modules Az

param(
    [Parameter(Mandatory=$true)]
    [string]$Environment = "dev",
    
    [Parameter(Mandatory=$false)]
    [string]$Location = "southeastasia",
    
    [Parameter(Mandatory=$false)]
    [string]$ResourceGroup = "rag-resource-group"
)

# Login to Azure
Write-Host "Logging in to Azure..."
az login

# Create resource group if not exists
Write-Host "Creating resource group..."
az group create --name $ResourceGroup --location $Location

# Deploy infrastructure
Write-Host "Deploying infrastructure..."
az deployment group create `
    --resource-group $ResourceGroup `
    --template-file ../infrastructure/main.bicep `
    --parameters environment=$Environment location=$Location

# Build and deploy function
Write-Host "Building function..."
Set-Location ../functions/ingestion-function
mvn clean package

Write-Host "Deploying function..."
mvn azure-functions:deploy `
    -DresourceGroup=$ResourceGroup `
    -DappName="rag-ingestion-function-$Environment" `
    -Dregion=$Location

Write-Host "Deployment completed successfully!"
