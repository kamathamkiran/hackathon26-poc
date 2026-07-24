package com.db.hackathon.agents.review;

import com.db.hackathon.model.review.ReviewIssue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ReviewAnalyzer {

    private static final double ERROR_PENALTY = 0.05;

    private static final double MAX_VALIDATION_PENALTY = 0.50;

    public List<ReviewIssue> diff(JsonNode original, JsonNode reviewed) {
        List<ReviewIssue> issues = new ArrayList<>();
        walkDiff(original, reviewed, "", issues);
        log.debug("Review diff produced {} field change(s)", issues.size());
        return issues;
    }

    private void walkDiff(JsonNode original, JsonNode reviewed, String path, List<ReviewIssue> issues) {
        if (reviewed == null) {
            return;
        }

        if (isLeaf(reviewed)) {
            String before = value(original);
            String after = value(reviewed);
            if (!before.equals(after)) {
                log.debug("Review changed field '{}' from '{}' to '{}'", path, before, after);
                issues.add(ReviewIssue.builder()
                        .field(path)
                        .previousValue(before)
                        .updatedValue(after)
                        .confidence(confidence(reviewed))
                        .pageNumber(pageNumber(reviewed))
                        .message(before.isEmpty()
                                ? "Review populated a value that extraction had left blank"
                                : "Review updated the value to match the source document")
                        .build());
            }
            return;
        }

        if (reviewed.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = reviewed.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode originalChild = original == null ? null : original.get(entry.getKey());
                String childPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                walkDiff(originalChild, entry.getValue(), childPath, issues);
            }
        } else if (reviewed.isArray()) {
            for (int i = 0; i < reviewed.size(); i++) {
                JsonNode originalChild = original != null && original.isArray() && i < original.size()
                        ? original.get(i) : null;
                walkDiff(originalChild, reviewed.get(i), path + "[" + i + "]", issues);
            }
        }
    }

    public double overallConfidence(JsonNode reviewed, int validationErrorCount) {
        double[] accumulator = {0.0, 0.0};
        accumulateConfidence(reviewed, accumulator);

        double leafCount = accumulator[1];
        double base = leafCount == 0 ? 0.0 : accumulator[0] / leafCount;

        double penalty = Math.min(MAX_VALIDATION_PENALTY, validationErrorCount * ERROR_PENALTY);
        double overall = clamp(base * (1 - penalty));
        double rounded = BigDecimal.valueOf(overall).setScale(2, RoundingMode.HALF_UP).doubleValue();

        log.debug("Overall confidence computed: base={}, leaves={}, errors={}, penalty={}, result={}",
                base, (long) leafCount, validationErrorCount, penalty, rounded);
        return rounded;
    }

    private void accumulateConfidence(JsonNode node, double[] accumulator) {
        if (node == null) {
            return;
        }
        if (isLeaf(node)) {
            boolean present = !value(node).isEmpty();
            Double confidence = confidence(node);
            accumulator[0] += present && confidence != null ? confidence : 0.0;
            accumulator[1] += 1;
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> accumulateConfidence(entry.getValue(), accumulator));
        } else if (node.isArray()) {
            node.forEach(child -> accumulateConfidence(child, accumulator));
        }
    }

    private boolean isLeaf(JsonNode node) {
        return node == null || node.isNull() || (node.isObject() && node.has("value"));
    }

    private String value(JsonNode leaf) {
        if (leaf == null) {
            return "";
        }
        JsonNode value = leaf.get("value");
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private Double confidence(JsonNode leaf) {
        JsonNode confidence = leaf == null ? null : leaf.get("confidence");
        return confidence == null || confidence.isNull() ? null : confidence.asDouble();
    }

    private Integer pageNumber(JsonNode leaf) {
        JsonNode page = leaf == null ? null : leaf.get("pageNumber");
        return page == null || page.isNull() ? null : page.asInt();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
