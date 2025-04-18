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

import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Slf4j
public class Function {
    private static final String STORAGE_CONNECTION_STRING = System.getenv("AzureWebJobsStorage");
    private static final String PROCESSING_QUEUE_NAME = "document-processing-queue";
    private static final String RAW_DOCUMENTS_CONTAINER = "raw-documents";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/html"
    );
    
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
            Map<String, Object> eventData = objectMapper.readValue(event, Map.class);
            String blobUrl = extractBlobUrl(eventData);
            
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(RAW_DOCUMENTS_CONTAINER);
            BlobClient blobClient = containerClient.getBlobClient(extractBlobName(blobUrl));
            
            if (!isValidDocument(blobClient)) {
                log.error("Invalid document: {}", blobUrl);
                return;
            }

            DocumentMetadata metadata = extractMetadata(blobClient);
            ProcessingMessage message = createProcessingMessage(blobClient, metadata);
            output.setValue(objectMapper.writeValueAsString(message));
            
            log.info("Successfully queued document for processing: {}", blobUrl);
        } catch (Exception e) {
            log.error("Error processing document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process document", e);
        }
    }

    private String extractBlobUrl(Map<String, Object> eventData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) eventData.get("data");
        if (data == null) {
            throw new IllegalArgumentException("Event data is missing 'data' field");
        }
        
        String url = (String) data.get("url");
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Event data is missing 'url' field");
        }
        
        return url;
    }

    private String extractBlobName(String blobUrl) {
        try {
            URL url = new URL(blobUrl);
            String path = url.getPath();
            // Remove container name from path
            return path.substring(path.indexOf('/', 1) + 1);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid blob URL: " + blobUrl, e);
        }
    }

    private boolean isValidDocument(BlobClient blobClient) {
        try {
            if (!blobClient.exists()) {
                log.error("Blob does not exist: {}", blobClient.getBlobUrl());
                return false;
            }

            long size = blobClient.getProperties().getBlobSize();
            if (size > MAX_FILE_SIZE) {
                log.error("Document size {} exceeds maximum allowed size {}", size, MAX_FILE_SIZE);
                return false;
            }

            String contentType = blobClient.getProperties().getContentType();
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                log.error("Content type {} is not allowed", contentType);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating document: {}", e.getMessage(), e);
            return false;
        }
    }

    private DocumentMetadata extractMetadata(BlobClient blobClient) {
        try {
            return DocumentMetadata.builder()
                    .fileName(blobClient.getBlobName())
                    .fileType(blobClient.getProperties().getContentType())
                    .fileSize(blobClient.getProperties().getBlobSize())
                    .uploadDate(Instant.now())
                    .blobUrl(blobClient.getBlobUrl())
                    .contentType(blobClient.getProperties().getContentType())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract metadata", e);
        }
    }

    private ProcessingMessage createProcessingMessage(BlobClient blobClient, DocumentMetadata metadata) {
        return ProcessingMessage.builder()
                .blobUrl(blobClient.getBlobUrl())
                .metadata(metadata)
                .processingStatus("PENDING")
                .build();
    }
} 