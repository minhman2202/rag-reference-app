package com.zuhlke.rag.ingestion;

import com.microsoft.azure.functions.OutputBinding;

public class QueueOutputBinding implements OutputBinding<String> {
    private String value;
    
    @Override
    public void setValue(String value) {
        this.value = value;
    }
    
    @Override
    public String getValue() {
        return value;
    }
} 