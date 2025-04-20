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

# Check prerequisites
check_prerequisites() {
    if ! command -v az &> /dev/null; then
        echo "Azure CLI is not installed. Please install it first."
        exit 1
    fi

    if ! command -v mvn &> /dev/null; then
        echo "Maven is not installed. Please install it first."
        exit 1
    fi

    if ! command -v jq &> /dev/null; then
        echo "jq is not installed. Please install it first."
        exit 1
    fi
}

# Clean up old deployments
cleanup_old_deployments() {
    echo "Cleaning up old deployments..."
    az deployment group delete \
        --name main \
        --resource-group $RESOURCE_GROUP \
        --yes || true
}

# Deploy infrastructure
deploy_infrastructure() {
    echo "Deploying infrastructure..."
    DEPLOYMENT_OUTPUT=$(az deployment group create \
        --resource-group $RESOURCE_GROUP \
        --template-file "$PROJECT_ROOT/infrastructure/main.bicep" \
        --parameters environment=$ENVIRONMENT)

    if [ $? -ne 0 ]; then
        echo "Infrastructure deployment failed"
        exit 1
    fi

    # Extract function app URL from outputs
    FUNCTION_APP_URL=$(echo $DEPLOYMENT_OUTPUT | jq -r '.properties.outputs.functionAppUrl.value')
    if [ -z "$FUNCTION_APP_URL" ]; then
        echo "Failed to get function app URL"
        exit 1
    fi
    echo "Function App URL: $FUNCTION_APP_URL"
}

# Build and deploy function
deploy_function() {
    echo "Building and deploying function app..."
    cd "$PROJECT_ROOT/functions/ingestion-function"
    
    # Get storage connection string
    STORAGE_CONNECTION_STRING=$(az storage account show-connection-string \
        --name "ragstorageinqs4uargpjkc" \
        --resource-group $RESOURCE_GROUP \
        --query connectionString \
        --output tsv)
    if [ -z "$STORAGE_CONNECTION_STRING" ]; then
        echo "Failed to get storage connection string"
        exit 1
    fi
    
    # Clean previous build
    rm -rf target
    
    # Build and deploy
    if ! mvn clean package azure-functions:deploy \
        -Denvironment=$ENVIRONMENT \
        -DresourceGroup=$RESOURCE_GROUP \
        -Dregion=$LOCATION \
        -DstorageConnectionString="$STORAGE_CONNECTION_STRING" \
        -DskipTests; then
        echo "Function deployment failed"
        exit 1
    fi
}

# Main deployment flow
main() {
    check_prerequisites
    
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

    # Clean up any soft-deleted resources
    echo "Cleaning up soft-deleted resources..."
    az cognitiveservices account purge \
        --name "rag-doc-intelligence-$ENVIRONMENT" \
        --resource-group $RESOURCE_GROUP \
        --location $LOCATION || true

    cleanup_old_deployments
    deploy_infrastructure
    deploy_function

    echo "Deployment completed successfully!"
    echo "Function App URL: $FUNCTION_APP_URL"
}

# Run main function
main 