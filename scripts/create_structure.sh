#!/bin/bash

# Create main directories
mkdir -p .github/workflows
mkdir -p frontend/{public,src/{components/{Chat,DocumentViewer,Search},pages,services,utils}}
mkdir -p backend/src/{main/{java/com/zuhlke/rag/{api/{controllers,models/{request,response},config},rag/{service/impl,model,config},search/{service,model},common},resources},test}
mkdir -p functions/{ingestion-function,processing-function}/src/{main/{java/com/zuhlke/rag/{ingestion,processing},resources},test}
mkdir -p infrastructure/modules
mkdir -p scripts

# Create empty files
touch frontend/package.json
touch frontend/vite.config.js
touch backend/pom.xml
touch backend/Dockerfile
touch functions/ingestion-function/pom.xml
touch functions/processing-function/pom.xml
touch infrastructure/main.bicep
touch infrastructure/modules/{storage,search,functions,webapp}.bicep
touch scripts/{deploy,setup}.ps1
touch README.md
touch azure-pipelines.yml

# Make script executable
chmod +x scripts/create_structure.sh 