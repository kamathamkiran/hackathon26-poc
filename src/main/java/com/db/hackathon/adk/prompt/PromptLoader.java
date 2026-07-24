package com.db.hackathon.adk.prompt;

import com.db.hackathon.enums.AgentType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptLoader {

    public String load(AgentType agentType) {

        String file = switch (agentType) {

            case DOCUMENT_PARSER -> "document.md";

            case EXTRACTION -> "extraction.md";

            case VALIDATION -> "validation.md";

            case REVIEW -> "review.md";

            case HUMAN_REVIEW -> null;
        };

        try {

            ClassPathResource resource =
                    new ClassPathResource("prompts/" + file);

            return new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

}
