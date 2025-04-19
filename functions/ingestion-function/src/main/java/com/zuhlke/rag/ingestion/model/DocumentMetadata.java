package com.zuhlke.rag.ingestion.model;

import lombok.Data;

@Data
public class DocumentMetadata {
    private String fileName;
    private String contentType;
    private long size;
    private String source;
} 