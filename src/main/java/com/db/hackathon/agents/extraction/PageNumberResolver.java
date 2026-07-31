package com.db.hackathon.agents.extraction;

import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.model.document.DocumentPage;
import com.db.hackathon.model.extraction.Deal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the {@code pageNumber} of every extracted field by locating its {@code sourceText}
 * within the parsed document pages. Used when the extraction model cannot reliably determine the
 * page number itself (for example when the PDF is sent directly to the LLM).
 *
 * <p>The traversal is schema-agnostic: it walks the serialized JSON tree of the {@link Deal} and
 * updates any object that carries both {@code sourceText} and {@code pageNumber}, so it works for
 * all nested objects and lists without knowing the model shape.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PageNumberResolver {

    private static final String SOURCE_TEXT = "sourceText";
    private static final String PAGE_NUMBER = "pageNumber";

    private final ObjectMapper objectMapper;

    /**
     * Returns a copy of {@code deal} with {@code pageNumber} corrected on every extracted field
     * whose {@code sourceText} can be located within the document pages.
     */
    public Deal resolve(Deal deal, DocumentAnalysis analysis) {
        if (deal == null) {
            log.debug("Page-number resolution skipped: deal is null");
            return null;
        }
        if (analysis == null || analysis.getPages() == null || analysis.getPages().isEmpty()) {
            log.warn("Page-number resolution skipped: no document pages available");
            return deal;
        }

        List<NormalizedPage> pages = normalizePages(analysis.getPages());
        if (pages.isEmpty()) {
            log.warn("Page-number resolution skipped: all pages are blank");
            return deal;
        }

        log.info("Resolving page numbers across {} non-blank page(s)", pages.size());

        try {
            JsonNode root = objectMapper.valueToTree(deal);
            int[] resolved = {0};
            traverse(root, pages, resolved);
            Deal updated = objectMapper.treeToValue(root, Deal.class);
            log.info("Page-number resolution completed: {} field(s) updated from sourceText", resolved[0]);
            return updated;
        } catch (Exception e) {
            log.error("Page-number resolution failed; returning original deal", e);
            return deal;
        }
    }

    private void traverse(JsonNode node, List<NormalizedPage> pages, int[] resolved) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isArray()) {
            for (JsonNode element : node) {
                traverse(element, pages, resolved);
            }
            return;
        }

        if (!node.isObject()) {
            return;
        }

        ObjectNode object = (ObjectNode) node;

        if (object.hasNonNull(SOURCE_TEXT) && object.has(PAGE_NUMBER)) {
            String sourceText = object.get(SOURCE_TEXT).asText();
            Integer page = findPage(sourceText, pages);
            if (page != null) {
                object.put(PAGE_NUMBER, page);
                resolved[0]++;
                log.debug("Resolved pageNumber={} for sourceText='{}'", page, sourceText);
            } else {
                log.debug("Could not locate sourceText in any page: '{}'", sourceText);
            }
        }

        object.fields().forEachRemaining(entry -> traverse(entry.getValue(), pages, resolved));
    }

    private Integer findPage(String sourceText, List<NormalizedPage> pages) {
        String needle = normalize(sourceText);
        if (needle.isEmpty()) {
            return null;
        }

        String needleAlnum = alphanumeric(needle);

        // Strategy 1: whitespace-normalized substring match.
        for (NormalizedPage page : pages) {
            if (page.text().contains(needle)) {
                return page.pageNumber();
            }
        }

        // Strategy 2: alphanumeric-only substring match (ignores punctuation/symbols like $, &).
        if (needleAlnum.length() >= 3) {
            for (NormalizedPage page : pages) {
                if (page.textAlnum().contains(needleAlnum)) {
                    return page.pageNumber();
                }
            }
        }

        // Strategy 3: token-overlap scoring, pick the best page above a threshold.
        List<String> tokens = tokenize(needle);
        if (tokens.isEmpty()) {
            return null;
        }

        Integer bestPage = null;
        double bestScore = 0.0;
        for (NormalizedPage page : pages) {
            long matched = tokens.stream().filter(t -> page.text().contains(t)).count();
            double score = (double) matched / tokens.size();
            if (score > bestScore) {
                bestScore = score;
                bestPage = page.pageNumber();
            }
        }

        if (bestScore >= 0.6) {
            log.debug("Token-overlap match (score={}) resolved page {} for '{}'", bestScore, bestPage, sourceText);
            return bestPage;
        }

        return null;
    }

    private List<String> tokenize(String normalized) {
        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split(" ")) {
            if (token.length() >= 4) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String alphanumeric(String text) {
        return text.replaceAll("[^a-z0-9]", "");
    }

    private List<NormalizedPage> normalizePages(List<DocumentPage> pages) {
        List<NormalizedPage> normalized = new ArrayList<>();
        for (DocumentPage page : pages) {
            if (page.getText() == null) {
                continue;
            }
            String text = normalize(page.getText());
            normalized.add(new NormalizedPage(page.getPageNumber(), text, alphanumeric(text)));
        }
        return normalized;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private record NormalizedPage(Integer pageNumber, String text, String textAlnum) {
    }
}
