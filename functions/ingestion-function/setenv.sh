#!/bin/bash

# Exit on error
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Function to print command execution
print_command() {
    echo -e "${BLUE}Executing:${NC} $1"
    echo "----------------------------------------"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check required tools
check_requirements() {
    local missing=0
    
    if ! command_exists mvn; then
        echo -e "${RED}Error: Maven is not installed${NC}"
        missing=1
    fi
    
    if ! command_exists java; then
        echo -e "${RED}Error: Java is not installed${NC}"
        missing=1
    fi
    
    if ! command_exists az; then
        echo -e "${RED}Error: Azure CLI is not installed${NC}"
        missing=1
    fi
    
    if [ $missing -eq 1 ]; then
        echo -e "${RED}Please install the missing tools before continuing${NC}"
        exit 1
    fi
}

# Set environment variables
setup_environment() {
    # Check if Azure is logged in
    if ! az account show &>/dev/null; then
        echo -e "${BLUE}Logging in to Azure...${NC}"
        az login
    fi
    
    # Set default subscription
    SUBSCRIPTION_ID=$(az account show --query id -o tsv)
    az account set --subscription $SUBSCRIPTION_ID
    
    # Set storage connection string for tests
    if [ -z "$AZURE_STORAGE_CONNECTION_STRING" ]; then
        echo -e "${BLUE}Setting up test storage account...${NC}"
        RESOURCE_GROUP="rag-resource-group"
        STORAGE_ACCOUNT=$(az storage account list --resource-group $RESOURCE_GROUP --query "[0].name" -o tsv)
        export AZURE_STORAGE_CONNECTION_STRING=$(az storage account show-connection-string --name $STORAGE_ACCOUNT --resource-group $RESOURCE_GROUP --query "connectionString" -o tsv)
    fi
    
    # Set Java home if not set
    if [ -z "$JAVA_HOME" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 17)
    fi
    
    # Add Maven to PATH if not already there
    if ! command_exists mvn; then
        export PATH=$PATH:/usr/local/apache-maven/bin
    fi
}

# Build commands
build() {
    print_command "mvn clean install -DskipTests"
    mvn clean install -DskipTests
}

test() {
    print_command "mvn test"
    mvn test
}

integration_test() {
    print_command "mvn verify -P integration-test"
    mvn verify -P integration-test
}

all_tests() {
    print_command "mvn verify"
    mvn verify
}

build_and_test() {
    print_command "mvn clean verify"
    mvn clean verify
}

# Show help
show_help() {
    echo -e "${GREEN}Available commands:${NC}"
    echo "  build          - Build without tests"
    echo "  test           - Run unit tests"
    echo "  integration    - Run integration tests"
    echo "  all            - Run all tests"
    echo "  build-test     - Build and run all tests"
    echo "  help           - Show this help"
}

# Main setup
echo -e "${GREEN}Setting up development environment...${NC}"
check_requirements
setup_environment
echo -e "${GREEN}Environment setup complete!${NC}"
echo -e "${GREEN}Available commands:${NC}"
echo "  build          - Build without tests"
echo "  test          - Run unit tests"
echo "  integration   - Run integration tests"
echo "  all           - Run all tests"
echo "  build-test    - Build and run all tests"
echo "  help          - Show this help"

# Export functions for use in the shell
export -f build test integration_test all_tests build_and_test show_help 