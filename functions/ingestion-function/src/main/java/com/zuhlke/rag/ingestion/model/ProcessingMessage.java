package com.zuhlke.rag.ingestion.model;

import lombok.Data;

@Data
public class ProcessingMessage {
    private DocumentMetadata metadata;
    private String operationLocation;
    private String status;
} 