package com.db.hackathon.service;

import com.db.hackathon.dto.WorkflowContext;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfSourceServiceTest {

    private final Storage storage = mock(Storage.class);
    private final PdfSourceService pdfSourceService = new PdfSourceService(storage);

    @Test
    void loadsAndCachesPdfBytesFromStorage() {
        byte[] expected = { 1, 2, 3 };
        Blob blob = mock(Blob.class);
        WorkflowContext context = WorkflowContext.builder()
                .sourceBucket("agreements")
                .sourceFilePath("agreement.pdf")
                .build();
        when(storage.get("agreements", "agreement.pdf")).thenReturn(blob);
        when(blob.getContent()).thenReturn(expected);

        assertArrayEquals(expected, pdfSourceService.loadPdf(context));
        assertArrayEquals(expected, pdfSourceService.loadPdf(context));

        verify(storage, times(1)).get("agreements", "agreement.pdf");
    }

    @Test
    void rejectsMissingPdfLocation() {
        WorkflowContext context = WorkflowContext.builder().build();

        assertThrows(IllegalStateException.class, () -> pdfSourceService.loadPdf(context));
    }
}
