package com.db.hackathon.adk.agent.document;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class DocumentAiConfiguration {

        private final DocumentAiProperties properties;

        @Bean(destroyMethod = "close")
        public DocumentProcessorServiceClient documentProcessorServiceClient() throws IOException {

                // 1. Load credentials JSON from src/main/resources
                ClassPathResource resource = new ClassPathResource(properties.getCredentials());
                GoogleCredentials credentials;

                try (InputStream inputStream = resource.getInputStream()) {
                        credentials = GoogleCredentials.fromStream(inputStream)
                                // CRITICAL: Attach GCP scope to authorize gRPC channel
                                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
                }

                // 2. Build explicit regional endpoint (asia-south1-documentai.googleapis.com:443)
                String endpoint = String.format("%s-documentai.googleapis.com:443", properties.getLocation());

                // 3. Build gRPC settings with scoped credentials
                DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
                        .setEndpoint(endpoint)
                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                        .build();

                return DocumentProcessorServiceClient.create(settings);
        }
}