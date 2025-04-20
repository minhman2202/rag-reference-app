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

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

public class DocumentIngestFunction {

    private static final Set<String> SUPPORTED_TYPES = Set.of(".pdf", ".docx", ".pptx", ".xlsx", ".txt", ".html");
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClients.createDefault();

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

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Ocp-Apim-Subscription-Key", apiKey);

            try (ClassicHttpResponse httpResponse = httpClient.execute(get, r -> r)) {
                String responseJson = new String(httpResponse.getEntity().getContent().readAllBytes());
                var jsonNode = mapper.readTree(responseJson);
                String status = jsonNode.get("status").asText();

                if ("succeeded".equalsIgnoreCase(status)) {
                    logger.info("Document analysis succeeded.");
                    return responseJson;
                } else if ("failed".equalsIgnoreCase(status)) {
                    logger.warning("Document analysis failed.");
                    break;
                }

                logger.info("Waiting for analysis to complete...");
                Thread.sleep(delayMs);
            }
        }

        throw new RuntimeException("Document analysis did not complete in time.");
    }

    private boolean isSupportedFile(String fileName) {
        return SUPPORTED_TYPES.stream().anyMatch(fileName.toLowerCase()::endsWith);
    }
}
