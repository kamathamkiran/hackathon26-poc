package com.db.hackathon.adk.agent;

import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.workflow.WorkflowContext;
import com.db.hackathon.model.document.DocumentPage;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DocumentParserAgent
        implements WorkflowAgent<WorkflowContext, WorkflowContext> {

    @Override
    public WorkflowContext process(
            WorkflowContext context) throws IOException {

        log.info("Starting document parsing...");

        List<DocumentPage> pages = extractPages(
                context.getPdf().getBytes());

        DocumentAnalysis analysis =
                DocumentAnalysis.builder()
                        .pages(pages)
                        .totalPages(pages.size())
                        .build();

        context.setDocumentAnalysis(analysis);

        log.info(String.valueOf(analysis));
        log.info("Document parsing completed. Total Pages={}",
                pages.size());

        return context;

    }

    private List<DocumentPage> extractPages(
            byte[] pdfBytes) throws IOException {

        List<DocumentPage> pages =
                new ArrayList<>();

        try (PDDocument document =
                     Loader.loadPDF(pdfBytes)) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            int totalPages =
                    document.getNumberOfPages();

            for (int page = 1;
                 page <= totalPages;
                 page++) {

                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String text =
                        stripper.getText(document);

                String cleaned = clean(text);

                pages.add(
                        DocumentPage.builder()
                                .pageNumber(page)
                                .text(cleaned)
                                .characterCount(cleaned.length())
                                .blankPage(cleaned.isBlank())
                                .build()
                );

            }

        }

        return pages;

    }

    private String clean(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("\u0000", "")
                .replaceAll("\\s+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

    }

}