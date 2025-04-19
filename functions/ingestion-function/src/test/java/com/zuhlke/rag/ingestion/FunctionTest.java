package com.zuhlke.rag.ingestion;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.zuhlke.rag.ingestion.model.ProcessingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class FunctionTest {
    private static final String TEST_CONTAINER = "raw-documents";
    private static final String TEST_QUEUE = "document-processing-queue";
    
    private BlobServiceClient blobServiceClient;
    private QueueClient queueClient;
    private Function function;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setup() {
        String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
        assertNotNull(connectionString, "AZURE_STORAGE_CONNECTION_STRING environment variable must be set");
        
        blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
            
        queueClient = new QueueClientBuilder()
            .connectionString(connectionString)
            .queueName(TEST_QUEUE)
            .buildClient();
            
        function = new Function();
        
        // Create container if it doesn't exist
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(TEST_CONTAINER);
        if (!container.exists()) {
            container.create();
        }
    }
    
    @Test
    void testValidDocumentProcessing() throws Exception {
        // Create test PDF file
        File testFile = createTestPdf();
        
        // Upload test file
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(TEST_CONTAINER);
        BlobClient blob = container.getBlobClient("test.pdf");
        blob.uploadFromFile(testFile.getPath());
        
        // Create mock event
        String event = createMockEvent(blob.getBlobUrl());
        
        // Execute function
        function.run(event, new QueueOutputBinding(), mock(ExecutionContext.class));
        
        // Verify queue message
        QueueMessageItem message = queueClient.receiveMessage();
        assertNotNull(message);
        
        ProcessingMessage processingMessage = new ObjectMapper()
            .readValue(message.getMessageText(), ProcessingMessage.class);
        
        assertEquals("PENDING", processingMessage.getProcessingStatus());
        assertEquals(blob.getBlobUrl(), processingMessage.getBlobUrl());
    }
    
    @Test
    void testInvalidContentType() throws Exception {
        // Create test file with invalid type
        File testFile = createTestFile("test.exe", 1024);
        
        // Upload test file
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(TEST_CONTAINER);
        BlobClient blob = container.getBlobClient("test.exe");
        blob.uploadFromFile(testFile.getPath());
        
        // Create mock event
        String event = createMockEvent(blob.getBlobUrl());
        
        // Execute function
        function.run(event, new QueueOutputBinding(), mock(ExecutionContext.class));
        
        // Verify no message in queue
        assertNull(queueClient.receiveMessage());
    }
    
    @Test
    void testLargeFile() throws Exception {
        // Create large file
        File largeFile = createTestFile("large.pdf", 51 * 1024 * 1024); // 51MB
        
        // Upload
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(TEST_CONTAINER);
        BlobClient blob = container.getBlobClient("large.pdf");
        blob.uploadFromFile(largeFile.getPath());
        
        // Create mock event
        String event = createMockEvent(blob.getBlobUrl());
        
        // Execute function
        function.run(event, new QueueOutputBinding(), mock(ExecutionContext.class));
        
        // Verify no message in queue
        assertNull(queueClient.receiveMessage());
    }
    
    private File createTestPdf() throws IOException {
        File file = new File(tempDir.toFile(), "test.pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Create a minimal valid PDF
            String pdfContent = "%PDF-1.4\n1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n2 0 obj\n<</Type /Pages /Kids [3 0 R] /Count 1>>\nendobj\n3 0 obj\n<</Type /Page /Parent 2 0 R /Resources <<>> /MediaBox [0 0 612 792]>>\nendobj\nxref\n0 4\n0000000000 65535 f\n0000000010 00000 n\n0000000056 00000 n\n0000000102 00000 n\ntrailer\n<</Size 4 /Root 1 0 R>>\nstartxref\n149\n%%EOF";
            fos.write(pdfContent.getBytes());
        }
        return file;
    }
    
    private File createTestFile(String name, long size) throws IOException {
        File file = new File(tempDir.toFile(), name);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            Arrays.fill(buffer, (byte) 0);
            for (long i = 0; i < size / 1024; i++) {
                fos.write(buffer);
            }
        }
        return file;
    }
    
    private String createMockEvent(String blobUrl) throws Exception {
        Map<String, Object> data = Map.of(
            "url", blobUrl
        );
        
        Map<String, Object> event = Map.of(
            "data", data
        );
        
        return new ObjectMapper().writeValueAsString(event);
    }
} 