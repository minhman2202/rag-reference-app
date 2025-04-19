# RAG Reference Application

A document management system leveraging Azure AI Foundry platform for intelligent document processing and retrieval.

## Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            Data Ingestion Layer                         │
└─────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            Processing Layer                             │
└─────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            Search & Storage Layer                       │
└─────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            Application Layer                            │
└─────────────────────────────────────────────────────────────────────────┘
```

### Components

1. **Data Ingestion Layer**
   - Azure Blob Storage for document storage
   - Azure Event Grid for document upload events
   - Azure Function for document ingestion and validation

2. **Processing Layer**
   - Azure Document Intelligence for document analysis
   - Azure Function for document processing and chunking
   - Azure Queue Storage for processing queue

3. **Search & Storage Layer**
   - Azure AI Search for document indexing and search
   - Vector embeddings for semantic search
   - Hybrid search (keyword + vector)

4. **Application Layer**
   - React-based web application
   - Spring Boot backend API
   - Azure OpenAI integration for RAG
   - Chatbot interface

## Prerequisites

- Java 17
- Node.js 18+
- Azure CLI
- Azure Functions Core Tools
- Maven
- Bash shell

## Getting Started

### Local Development

1. Clone the repository
2. Install dependencies:
   ```bash
   # Backend
   cd backend
   mvn install

   # Frontend
   cd frontend
   npm install
   ```

3. Configure environment variables:
   ```bash
   cp .env.example .env
   # Edit .env with your Azure credentials
   ```

4. For Functions module, navigate to the function directory and use the following commands:
   ```bash
   # Build without tests
   mvn clean install -DskipTests

   # Run unit tests
   mvn test

   # Run integration tests
   mvn verify -P integration-test

   # Run all tests
   mvn verify

   # Build and run all tests
   mvn clean verify
   ```
   
### Deployment

1. Install Azure CLI and login:
   ```bash
   az login
   ```

2. Make deployment script executable:
   ```bash
   chmod +x scripts/deploy.sh
   ```

3. Deploy infrastructure and function:
   ```bash
   # Deploy to dev environment
   ./scripts/deploy.sh -e dev

   # Deploy to specific region
   ./scripts/deploy.sh -e dev -l southeastasia

   # Deploy with custom resource group
   ./scripts/deploy.sh -e dev -g my-resource-group
   ```

## Project Structure

```
rag-reference-app/
├── frontend/          # React frontend
├── backend/           # Spring Boot API
├── functions/         # Azure Functions
│   ├── ingestion-function/
│   │   ├── src/      # Source code
│   │   ├── pom.xml   # Maven configuration
│   │   └── setenv.sh # Development environment setup
│   └── processing-function/
├── infrastructure/    # IaC (Bicep)
└── scripts/           # Deployment scripts
```

## Functions Module

### Development Setup

1. Navigate to the function directory:
   ```bash
   cd functions/ingestion-function
   ```

2. Set up development environment:
   ```bash
   source setenv.sh
   ```

3. Available commands after sourcing setenv.sh:
   ```bash
   build          # Build without tests
   test          # Run unit tests
   integration   # Run integration tests
   all           # Run all tests
   build-test    # Build and run all tests
   help          # Show help
   ```

### Testing

- Unit tests run by default with `mvn test`
- Integration tests require Azure Storage connection string
- Integration tests are skipped by default in regular builds
- Run integration tests with `integration` command

### Deployment

1. Make sure you're logged into Azure:
   ```bash
   az login
   ```

2. Deploy using the deployment script:
   ```bash
   # From project root
   ./scripts/deploy.sh -e dev
   ```

### Environment Variables

The following environment variables are set up by setenv.sh:
- `AZURE_STORAGE_CONNECTION_STRING`: For test storage account
- `JAVA_HOME`: Java 17 home directory
- `PATH`: Includes Maven if not already present

## Features

- Document upload and validation
- Automatic document processing
- Semantic search with RAG
- Document summarization
- Chatbot interface
- Document versioning

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
