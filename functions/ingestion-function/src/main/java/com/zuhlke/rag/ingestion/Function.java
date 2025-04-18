package com.zuhlke.rag.ingestion;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueOutput;
import com.zuhlke.rag.ingestion.model.DocumentMetadata;
import com.zuhlke.rag.ingestion.model.ProcessingMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class Function {
    private static final String STORAGE_CONNECTION_STRING = System.getenv("AzureWebJobsStorage");
    private static final String PROCESSING_QUEUE_NAME = "document-processing-queue";
    private static final String RAW_DOCUMENTS_CONTAINER = "raw-documents";
    
    private final BlobServiceClient blobServiceClient;
    private final QueueServiceClient queueServiceClient;
    private final ObjectMapper objectMapper;

    public Function() {
        if (STORAGE_CONNECTION_STRING == null || STORAGE_CONNECTION_STRING.isBlank()) {
            throw new IllegalStateException("AzureWebJobsStorage connection string is not configured");
        }
        
        try {
            this.blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(STORAGE_CONNECTION_STRING)
                    .buildClient();
            this.queueServiceClient = new QueueServiceClientBuilder()
                    .connectionString(STORAGE_CONNECTION_STRING)
                    .buildClient();
            this.objectMapper = new ObjectMapper();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Azure clients", e);
        }
    }

    @FunctionName("processDocument")
    public void run(
            @EventGridTrigger(name = "event") String event,
            @QueueOutput(name = "output", 
                        queueName = PROCESSING_QUEUE_NAME,
                        connection = "AzureWebJobsStorage") OutputBinding<String> output,
            final ExecutionContext context) {
        try {
            // TODO: Parse EventGrid event to get blob URL and metadata
            Map<String, Object> eventData = objectMapper.readValue(event, Map.class);
            String blobUrl = extractBlobUrl(eventData);
            
            // TODO: Validate document type and size
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(RAW_DOCUMENTS_CONTAINER);
            BlobClient blobClient = containerClient.getBlobClient(extractBlobName(blobUrl));
            
            if (!isValidDocument(blobClient)) {
                log.error("Invalid document: {}", blobUrl);
                return;
            }

            // TODO: Extract metadata
            DocumentMetadata metadata = extractMetadata(blobClient);

            // TODO: Create processing message
            ProcessingMessage message = createProcessingMessage(blobClient, metadata);

            // TODO: Send to processing queue
            output.setValue(objectMapper.writeValueAsString(message));
            
            log.info("Successfully queued document for processing: {}", blobUrl);
        } catch (Exception e) {
            log.error("Error processing document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process document", e);
        }
    }

    private String extractBlobUrl(Map<String, Object> eventData) {
        // TODO: Implement blob URL extraction from EventGrid event
        return null;
    }

    private String extractBlobName(String blobUrl) {
        // TODO: Implement blob name extraction from URL
        return null;
    }

    private boolean isValidDocument(BlobClient blobClient) {
        // TODO: Implement document validation
        // - Check file type (PDF, DOCX, etc.)
        // - Check file size
        // - Check if document is not corrupted
        return true;
    }

    private DocumentMetadata extractMetadata(BlobClient blobClient) {
        // TODO: Implement metadata extraction
        // - File name
        // - File type
        // - File size
        // - Upload date
        return null;
    }

    private ProcessingMessage createProcessingMessage(BlobClient blobClient, DocumentMetadata metadata) {
        // TODO: Create processing message with all necessary information
        return null;
    }
} 