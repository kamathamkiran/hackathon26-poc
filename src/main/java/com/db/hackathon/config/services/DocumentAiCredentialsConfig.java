package com.db.hackathon.config.services;

import com.db.hackathon.config.DocumentAiProperties;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class DocumentAiCredentialsConfig {

    private final DocumentAiProperties properties;
    private final GoogleCredentials googleCredentials;

    @Bean(destroyMethod = "close")
    public DocumentProcessorServiceClient documentProcessorServiceClient()
            throws IOException {

        String endpoint = String.format(
                "%s-documentai.googleapis.com:443",
                properties.getLocation());

        DocumentProcessorServiceSettings settings =
                DocumentProcessorServiceSettings.newBuilder()
                        .setEndpoint(endpoint)
                        .setCredentialsProvider(
                                FixedCredentialsProvider.create(googleCredentials))
                        .build();

        return DocumentProcessorServiceClient.create(settings);
    }
}