#!/bin/bash

# Exit on error
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Default values
ENVIRONMENT="dev"
LOCATION="southeastasia"
RESOURCE_GROUP="rag-resource-group"
FUNCTION="all" # all, ingestion, processing

# Parse command line arguments
while getopts "e:l:g:f:" opt; do
  case $opt in
    e) ENVIRONMENT="$OPTARG" ;;
    l) LOCATION="$OPTARG" ;;
    g) RESOURCE_GROUP="$OPTARG" ;;
    f) FUNCTION="$OPTARG" ;;
    \?) echo "Invalid option -$OPTARG" >&2; exit 1 ;;
  esac
done

echo "Deploying function with parameters:"
echo "Environment: $ENVIRONMENT"
echo "Location: $LOCATION"
echo "Resource Group: $RESOURCE_GROUP"
echo "Function: $FUNCTION"

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

deploy_ingestion_function() {
    echo "Building and deploying ingestion function..."
    cd "$PROJECT_ROOT/functions/ingestion-function"
    
    STORAGE_CONNECTION_STRING=$(get_storage_connection_string)
    
    rm -rf target
    
    if ! mvn clean package azure-functions:deploy \
        -Denvironment=$ENVIRONMENT \
        -DresourceGroup=$RESOURCE_GROUP \
        -Dregion=$LOCATION \
        -DstorageConnectionString="$STORAGE_CONNECTION_STRING" \
        -DskipTests; then
        echo "Ingestion function deployment failed"
        exit 1
    fi
}

deploy_processing_function() {
    echo "Building and deploying processing function..."
    cd "$PROJECT_ROOT/functions/processing-function"
    
    STORAGE_CONNECTION_STRING=$(get_storage_connection_string)
    
    rm -rf target
    
    if ! mvn clean package azure-functions:deploy \
        -Denvironment=$ENVIRONMENT \
        -DresourceGroup=$RESOURCE_GROUP \
        -Dregion=$LOCATION \
        -DstorageConnectionString="$STORAGE_CONNECTION_STRING" \
        -DskipTests; then
        echo "Processing function deployment failed"
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

    case $FUNCTION in
        "all")
            deploy_ingestion_function
            deploy_processing_function
            ;;
        "ingestion")
            deploy_ingestion_function
            ;;
        "processing")
            deploy_processing_function
            ;;
        *)
            echo "Invalid function specified. Use 'all', 'ingestion', or 'processing'"
            exit 1
            ;;
    esac

    echo "Function deployment completed successfully!"
}

main 