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
mvn clean package

echo "Deploying function..."
mvn azure-functions:deploy

echo "Deployment completed successfully!" 