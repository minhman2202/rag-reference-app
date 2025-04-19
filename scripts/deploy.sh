#!/bin/bash

# Default values
ENVIRONMENT="dev"
LOCATION="southeastasia"
RESOURCE_GROUP="rag-resource-group"

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -e|--environment)
      ENVIRONMENT="$2"
      shift 2
      ;;
    -l|--location)
      LOCATION="$2"
      shift 2
      ;;
    -g|--resource-group)
      RESOURCE_GROUP="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

# Login to Azure
echo "Logging in to Azure..."
az login

# Create resource group if not exists
echo "Creating resource group..."
az group create --name $RESOURCE_GROUP --location $LOCATION

# Deploy infrastructure
echo "Deploying infrastructure..."
az deployment group create \
    --resource-group $RESOURCE_GROUP \
    --template-file "$PROJECT_ROOT/infrastructure/main.bicep" \
    --parameters environment=$ENVIRONMENT location=$LOCATION

# Build and deploy function
echo "Building function..."
cd "$PROJECT_ROOT/functions/ingestion-function"
mvn clean package -Denvironment=$ENVIRONMENT

echo "Deploying function..."
mvn azure-functions:deploy \
    -DresourceGroup=$RESOURCE_GROUP \
    -DappName="rag-ingestion-function-$ENVIRONMENT" \
    -Dregion=$LOCATION \
    -Denvironment=$ENVIRONMENT

echo "Deployment completed successfully!" 