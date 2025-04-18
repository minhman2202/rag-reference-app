package com.zuhlke.rag.ingestion.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcessingMessage {
    private String blobUrl;
    private DocumentMetadata metadata;
    private String processingStatus;
} 