#!/bin/bash

# Exit on error
set -e

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

echo "Deploying functions with parameters:"
echo "Environment: $ENVIRONMENT"
echo "Location: $LOCATION"
echo "Resource Group: $RESOURCE_GROUP"

check_prerequisites() {
    if ! command -v az &> /dev/null; then
        echo "Azure CLI is not installed. Please install it first."
        exit 1
    fi

    if ! command -v mvn &> /dev/null; then
        echo "Maven is not installed. Please install it first."
        exit 1
    fi
}

get_storage_connection_string() {
    local connection_string=$(az storage account show-connection-string \
        --name "ragstorageinqs4uargpjkc" \
        --resource-group $RESOURCE_GROUP \
        --query connectionString \
        --output tsv)
    
    if [ -z "$connection_string" ]; then
        echo "Failed to get storage connection string"
        exit 1
    fi
    echo "$connection_string"
}

deploy_functions() {
    echo "Building and deploying functions..."
    cd "$PROJECT_ROOT/functions"
    
    STORAGE_CONNECTION_STRING=$(get_storage_connection_string)
    
    rm -rf target
    
    if ! mvn clean package azure-functions:deploy \
        -Denvironment=$ENVIRONMENT \
        -DresourceGroup=$RESOURCE_GROUP \
        -Dregion=$LOCATION \
        -DstorageConnectionString="$STORAGE_CONNECTION_STRING" \
        -DskipTests; then
        echo "Functions deployment failed"
        exit 1
    fi
}

main() {
    check_prerequisites
    
    echo "Logging in to Azure..."
    az login

    echo "Setting subscription..."
    SUBSCRIPTION_ID=$(az account show --query id -o tsv)
    az account set --subscription $SUBSCRIPTION_ID

    deploy_functions

    echo "Function deployment completed successfully!"
}

main 