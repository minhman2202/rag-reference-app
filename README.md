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
│   └── processing-function/
├── infrastructure/    # IaC (Bicep)
└── scripts/           # Deployment scripts
```


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
