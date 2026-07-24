package com.db.hackathon.agents.document;

import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.Document.Page;
import com.google.cloud.documentai.v1.Document.Page.Paragraph;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.model.document.DocumentPage;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import com.google.cloud.documentai.v1.Document.Page.Block;
import com.google.cloud.documentai.v1.Document.Page.Layout;
import com.google.cloud.documentai.v1.Document.TextAnchor.TextSegment;

@Component
public class DocumentAiMapper {

    public DocumentAnalysis map(Document document) {

        List<DocumentPage> pages = new ArrayList<>();

        for (Page page : document.getPagesList()) {

            String pageText = extractPageText(document, page);

            pages.add(
                    DocumentPage.builder()
                            .pageNumber(page.getPageNumber())
                            .text(pageText)
                            .blankPage(pageText.isBlank())
                            .build()
            );
        }

        return DocumentAnalysis.builder()
                .totalPages(document.getPagesCount())
                .fullText(document.getText())
                .pages(pages)
                .build();
    }

    /**
     * Prefer blocks because they preserve layout better.
     * Fallback to paragraphs if blocks are unavailable.
     */
    private String extractPageText(Document document, Page page) {

        StringBuilder builder = new StringBuilder();

        if (!page.getBlocksList().isEmpty()) {

            for (Block block : page.getBlocksList()) {

                builder.append(extractText(document, block.getLayout()))
                        .append("\n");

            }

        } else {

            for (Paragraph paragraph : page.getParagraphsList()) {

                builder.append(extractText(document, paragraph.getLayout()))
                        .append("\n");

            }

        }

        return builder.toString().trim();

    }

    /**
     * Converts Google's TextAnchor offsets into actual text.
     */
    private String extractText(Document document, Layout layout) {

        if (!layout.hasTextAnchor()) {
            return "";
        }

        String fullText = document.getText();

        StringBuilder builder = new StringBuilder();

        for (TextSegment segment :
                layout.getTextAnchor().getTextSegmentsList()) {

            int start = (int) segment.getStartIndex();

            int end = (int) segment.getEndIndex();

            if (start >= fullText.length()) {
                continue;
            }

            end = Math.min(end, fullText.length());

            builder.append(fullText, start, end);

        }

        return builder.toString();

    }

}
