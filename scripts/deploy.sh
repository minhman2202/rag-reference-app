#!/bin/bash

# Exit on error
set -e

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Default values
ENVIRONMENT="dev"
LOCATION="southeastasia"
RESOURCE_GROUP="rag-resource-group"

# Parse command line arguments
while getopts "e:l:g:" opt; do
  case $opt in
    e) ENVIRONMENT="$OPTARG" ;;
    l) LOCATION="$OPTARG" ;;
    g) RESOURCE_GROUP="$OPTARG" ;;
    \?) echo "Invalid option -$OPTARG" >&2; exit 1 ;;
  esac
done

echo "Deploying with parameters:"
echo "Environment: $ENVIRONMENT"
echo "Location: $LOCATION"
echo "Resource Group: $RESOURCE_GROUP"
echo "Function App Name: rag-ingestion-function-$ENVIRONMENT"

# Check if Azure CLI is installed
if ! command -v az &> /dev/null; then
    echo "Azure CLI is not installed. Please install it first."
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Please install it first."
    exit 1
fi

# Login to Azure
echo "Logging in to Azure..."
az login

# Set the subscription
echo "Setting subscription..."
SUBSCRIPTION_ID=$(az account show --query id -o tsv)
az account set --subscription $SUBSCRIPTION_ID

# Create resource group if it doesn't exist
echo "Creating resource group..."
az group create --name $RESOURCE_GROUP --location $LOCATION --output none

# Deploy infrastructure
echo "Deploying infrastructure..."
az deployment group create \
    --resource-group $RESOURCE_GROUP \
    --template-file "$PROJECT_ROOT/infrastructure/main.bicep" \
    --parameters environment=$ENVIRONMENT \
    --output none

# Build and deploy function
echo "Building function..."
cd "$PROJECT_ROOT/functions/ingestion-function"

# Get storage connection string
STORAGE_ACCOUNT_NAME=$(az storage account list --resource-group $RESOURCE_GROUP --query "[0].name" -o tsv)
STORAGE_CONNECTION_STRING=$(az storage account show-connection-string --name $STORAGE_ACCOUNT_NAME --resource-group $RESOURCE_GROUP --query "connectionString" -o tsv)

mvn clean package -Denvironment=$ENVIRONMENT -DstorageConnectionString="$STORAGE_CONNECTION_STRING"

echo "Deploying function..."
mvn azure-functions:deploy -Denvironment=$ENVIRONMENT -DstorageConnectionString="$STORAGE_CONNECTION_STRING"

# Wait for function app to be ready
echo "Waiting for function app to be ready..."
sleep 60

# Create Event Grid subscription
echo "Creating Event Grid subscription..."
STORAGE_ACCOUNT_NAME=$(az storage account list --resource-group $RESOURCE_GROUP --query "[0].name" -o tsv)
FUNCTION_APP_ID=$(az functionapp show --name "rag-ingestion-function-$ENVIRONMENT" --resource-group $RESOURCE_GROUP --query "id" -o tsv)

az eventgrid event-subscription create \
    --name "document-upload-subscription" \
    --source-resource-id "/subscriptions/$(az account show --query id -o tsv)/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.Storage/storageAccounts/$STORAGE_ACCOUNT_NAME" \
    --endpoint-type "azurefunction" \
    --endpoint "$FUNCTION_APP_ID/functions/processDocument" \
    --included-event-types "Microsoft.Storage.BlobCreated"

echo "Deployment completed successfully!" 