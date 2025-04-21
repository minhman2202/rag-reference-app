package com.zuhlke.rag.ingestion;

import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.BlobOutput;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

public class DocumentIngestFunction {

    private static final Set<String> SUPPORTED_TYPES = Set.of(".pdf", ".docx", ".pptx", ".xlsx", ".txt", ".html");
    private static final ObjectMapper mapper = new ObjectMapper();

    @FunctionName("BlobTriggerDocIngest")
    public void run(
        @BlobTrigger(name = "inputBlob", path = "incoming/{name}", dataType = "binary", connection = "AzureWebJobsStorage") byte[] inputBlob,
        @BindingName("name") String fileName,
        @BlobOutput(name = "outputBlob", path = "processed/{name}.json", connection = "AzureWebJobsStorage") OutputBinding<String> outputBlob,
        final ExecutionContext context
    ) {
        Logger logger = context.getLogger();
        logger.info("Triggered by blob: " + fileName + " (" + inputBlob.length + " bytes)");

        if (!isSupportedFile(fileName)) {
            logger.warning("Unsupported file type: " + fileName);
            return;
        }

        String apiKey = System.getenv("AZURE_DOC_INTELLIGENCE_KEY");
        String endpoint = System.getenv("AZURE_DOC_INTELLIGENCE_ENDPOINT");
        // Remove trailing slash if exists
        endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        
        String analyzeUrl = endpoint + "/formrecognizer/documentModels/prebuilt-layout:analyze?api-version=2023-07-31";

        try {
            String operationLocation = submitToDocumentIntelligence(inputBlob, analyzeUrl, apiKey, logger);
            String resultJson = pollAnalyzeResult(operationLocation, apiKey, logger);
            outputBlob.setValue(resultJson);
            logger.info("Analysis result saved to /processed/" + fileName + ".json");
        } catch (Exception e) {
            logger.severe("Document analysis failed: " + e.getMessage());
        }
    }

    private String submitToDocumentIntelligence(byte[] inputBlob, String analyzeUrl, String apiKey, Logger logger) throws IOException {
        HttpPost post = new HttpPost(analyzeUrl);
        post.setHeader("Ocp-Apim-Subscription-Key", apiKey);
        post.setHeader("Content-Type", "application/octet-stream");
        post.setEntity(new ByteArrayEntity(inputBlob, ContentType.APPLICATION_OCTET_STREAM));

        HttpClient httpClient = HttpClients.createDefault();
        try (ClassicHttpResponse httpResponse = httpClient.execute(post, r -> r)) {
            String operationLocation = Arrays.stream(httpResponse.getHeaders())
                    .filter(h -> h.getName().equalsIgnoreCase("Operation-Location"))
                    .map(h -> h.getValue())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Operation-Location header not found"));
            
            logger.info("Submitted to Document Intelligence. Operation-Location: " + operationLocation);
            return operationLocation;
        }
    }

    private String pollAnalyzeResult(String url, String apiKey, Logger logger) throws Exception {
      int maxRetries = 10;
      int delayMs = 2000;
  
      HttpClient httpClient = HttpClients.createDefault();
  
      for (int attempt = 1; attempt <= maxRetries; attempt++) {
          logger.info("Polling for analysis result... Attempt " + attempt);
  
          HttpGet get = new HttpGet(url);
          get.setHeader("Ocp-Apim-Subscription-Key", apiKey);
  
          try {
              String responseJson = httpClient.execute(get, httpResponse -> {
                  int statusCode = httpResponse.getCode();
                  logger.info("Received HTTP status code: " + statusCode);
  
                  if (statusCode == 429 || statusCode >= 500) {
                      logger.warning("Transient error (HTTP " + statusCode + "). Will retry.");
                      return null;
                  }
  
                  HttpEntity entity = httpResponse.getEntity();
                  if (entity == null) {
                      throw new IOException("No content returned from service.");
                  }
  
                  try (InputStream content = entity.getContent()) {
                      return new String(content.readAllBytes());
                  }
              });
  
              if (responseJson == null) {
                  // Retry logic
                  Thread.sleep(delayMs);
                  continue;
              }
  
              var jsonNode = mapper.readTree(responseJson);
              String status = jsonNode.get("status").asText();
  
              switch (status.toLowerCase()) {
                  case "succeeded":
                      logger.info("Document analysis succeeded.");
                      return responseJson;
                  case "failed":
                      logger.warning("Document analysis failed.");
                      throw new RuntimeException("Document analysis failed.");
                  default:
                      logger.info("Analysis not complete yet. Status: " + status);
              }
  
          } catch (Exception e) {
              logger.severe("Error during poll attempt " + attempt + ": " + e.getMessage());
              if (attempt == maxRetries) {
                  throw e;
              }
          }
  
          logger.info("Waiting " + delayMs + " ms before next attempt...");
          Thread.sleep(delayMs);
      }
  
      throw new RuntimeException("Document analysis did not complete within retry limit.");
  }
  

    private boolean isSupportedFile(String fileName) {
        return SUPPORTED_TYPES.stream().anyMatch(fileName.toLowerCase()::endsWith);
    }

}