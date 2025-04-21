package com.zuhlke.rag.processing;

import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.BlobOutput;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.ExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.util.logging.Logger;
import java.util.Collections;

public class IndexToAzureSearchFunction {
    private static final String SEARCH_ENDPOINT = System.getenv("AZURE_SEARCH_ENDPOINT");
    private static final String SEARCH_INDEX_NAME = System.getenv("AZURE_SEARCH_INDEX_NAME");
    private static final String SEARCH_ADMIN_KEY = System.getenv("AZURE_SEARCH_ADMIN_KEY");
  
    private static final ObjectMapper mapper = new ObjectMapper();

    @FunctionName("BlobTriggerIndexToSearch")
    public void run(
        @BlobTrigger(name = "inputBlob", path = "processed/{name}", dataType = "binary", connection = "AzureWebJobsStorage") byte[] inputBlob,
        @BindingName("name") String fileName,
        @BlobOutput(name = "failedBlob", path = "failed/{name}_{executionId}", connection = "AzureWebJobsStorage") OutputBinding<byte[]> failedBlob,
        final ExecutionContext context
    ) {
        Logger logger = context.getLogger();
        logger.info("Triggered by blob: " + fileName + " (" + inputBlob.length + " bytes)");

        try {
            // Parse Document Intelligence JSON
            JsonNode docIntelligenceResult = mapper.readTree(inputBlob);
            
            // Extract content and metadata
            String content = extractContent(docIntelligenceResult);
            String metadata = extractMetadata(docIntelligenceResult, fileName);
            
            SearchDocument searchDoc = buildSearchDocument(fileName, content, metadata);
            
            indexToAzureSearch(searchDoc, logger);
            
            logger.info("Successfully indexed document: " + fileName);
        } catch (Exception e) {
            logger.severe("Failed to index document: " + e.getMessage());
            failedBlob.setValue(inputBlob);
        }
    }

    private String extractContent(JsonNode docIntelligenceResult) {
      StringBuilder text = new StringBuilder();
        
      // Extract text from layout analysis
      JsonNode pages = docIntelligenceResult.path("analyzeResult").path("pages");
      if (pages == null || !pages.isArray()) {
          return text.toString();
      }

      for (JsonNode page : pages) {
          JsonNode lines = page.path("lines");
          for (JsonNode line : lines) {
              text.append(line.path("content").asText()).append("\n");
          }
      }
      
      return text.toString().trim();
    }

    private String extractMetadata(JsonNode docIntelligenceResult, String fileName) {
      StringBuilder metadata = new StringBuilder();
        
      // Add filename metadata
      metadata.append("filename: ").append(fileName.replace(".json", "")).append("\n");
      
      // Get the analyzeResult
      JsonNode analyzeResult = docIntelligenceResult.get("analyzeResult");
      if (analyzeResult == null) {
          return metadata.toString();
      }

      // Extract document type
      JsonNode docType = analyzeResult.get("docType");
      if (docType != null) {
          metadata.append("document_type: ").append(docType.asText()).append("\n");
      }

      // Extract page count
      JsonNode pages = analyzeResult.get("pages");
      if (pages != null && pages.isArray()) {
          metadata.append("page_count: ").append(pages.size()).append("\n");
      }

      // Extract creation date if available
      JsonNode createdDateTime = analyzeResult.get("createdDateTime");
      if (createdDateTime != null) {
          metadata.append("created_date: ").append(createdDateTime.asText()).append("\n");
      }

      return metadata.toString().trim();
    }

    private SearchDocument buildSearchDocument(String fileName, String content, String metadata) {
        SearchDocument doc = new SearchDocument();
        doc.id = UUID.randomUUID().toString();
        doc.fileName = fileName.replace(".json", "");
        doc.content = content;
        doc.metadata = metadata;
        // TODO: Add filepath
        // TODO: Add embedding
        doc.uploadDate = System.currentTimeMillis();
        return doc;
    }

    private void indexToAzureSearch(SearchDocument searchDoc, Logger logger) {
        try {
            SearchClient client = new SearchClientBuilder()
                .endpoint(SEARCH_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_ADMIN_KEY))
                .indexName(SEARCH_INDEX_NAME)
                .buildClient();

            client.uploadDocuments(Collections.singletonList(searchDoc));
            logger.info("Successfully indexed document: " + searchDoc.id);
        } catch (Exception e) {
            logger.severe("Failed to index document: " + e.getMessage());
            throw new RuntimeException("Failed to index document", e);
        }
    }

    @Setter
    @Getter
    private static class SearchDocument {
        // Getters and setters for Jackson serialization
        @JsonProperty("id")
        private String id;

        @JsonProperty("fileName")
        private String fileName;

        @JsonProperty("content")
        private String content;

        // @JsonProperty("embedding")
        // private String embedding;

        @JsonProperty("metadata")
        private String metadata;

        // @JsonProperty("filepath")
        // private String filepath;

        @JsonProperty("uploadDate")
        private long uploadDate;

    }

} 