package com.zuhlke.rag.ingestion.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DocumentMetadata {
    private String fileName;
    private String fileType;
    private long fileSize;
    private Instant uploadDate;
    private String blobUrl;
    private String contentType;
} 