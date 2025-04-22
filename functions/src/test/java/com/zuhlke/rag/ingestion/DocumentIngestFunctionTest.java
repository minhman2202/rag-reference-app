package com.zuhlke.rag.ingestion;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.logging.Logger;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
public class DocumentIngestFunctionTest {

    @Mock
    private ExecutionContext context;
    
    @Mock
    private Logger logger;
    
    @Mock
    private OutputBinding<String> outputBlob;
    
    @Mock
    private OutputBinding<byte[]> failedBlob;

    @Test
    public void testFunctionSetup() {
        // Setup
        when(context.getLogger()).thenReturn(logger);
        DocumentIngestFunction function = new DocumentIngestFunction();
        byte[] inputBlob = new byte[0];
        String fileName = "test.txt";

        // Execute
        function.run(inputBlob, fileName, outputBlob, failedBlob, context);

        // Verify basic function execution
        verify(context, atLeastOnce()).getLogger();
    }
} 