package com.db.hackathon.adk.prompt;

import com.db.hackathon.enums.AdkAgentType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PromptLoader {

    public String load(AdkAgentType agentType) {

        String file = switch (agentType) {

            case DOCUMENT -> "document.md";

            case EXTRACTION -> "extraction.md";

            case VALIDATION -> "validation.md";

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
