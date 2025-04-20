package com.zuhlke.rag.ingestion;

import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.ExecutionContext;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.ContentType;

import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

public class DocumentIngestFunction {

    private static final Set<String> SUPPORTED_TYPES = Set.of(".pdf", ".docx", ".pptx", ".xlsx", ".txt", ".html");

    @FunctionName("BlobTriggerDocIngest")
    public void run(
        @BlobTrigger(name = "content", path = "incoming/{name}", dataType = "binary", connection = "AzureWebJobsStorage") byte[] content,
        @BindingName("name") String fileName,
        final ExecutionContext context
    ) {
        Logger logger = context.getLogger();
        logger.info("Triggered by blob: " + fileName + " (" + content.length + " bytes)");

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
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(analyzeUrl);
            post.setHeader("Ocp-Apim-Subscription-Key", apiKey);
            post.setHeader("Content-Type", "application/octet-stream");
            post.setEntity(new ByteArrayEntity(content, ContentType.APPLICATION_OCTET_STREAM));

            HttpResponse response = client.execute(post, response1 -> response1);
            String operationLocation = Arrays.stream(response.getHeaders())
                                             .filter(h -> h.getName().equalsIgnoreCase("Operation-Location"))
                                             .map(h -> h.getValue())
                                             .findFirst().orElse("N/A");

            logger.info("Submitted to Document Intelligence. Operation-Location: " + operationLocation);
        } catch (Exception e) {
            logger.severe("Document analysis failed: " + e.getMessage());
        }
    }

    private boolean isSupportedFile(String fileName) {
        return SUPPORTED_TYPES.stream().anyMatch(fileName.toLowerCase()::endsWith);
    }
}
