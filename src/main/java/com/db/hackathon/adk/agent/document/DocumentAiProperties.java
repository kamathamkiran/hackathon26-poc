package com.db.hackathon.adk.agent.document;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google.document-ai")
@Data
public class DocumentAiProperties {

    private String projectId;

    private String location;

    private String processorId;

    private String credentials;

}
