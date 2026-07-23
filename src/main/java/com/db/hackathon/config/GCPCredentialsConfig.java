package com.db.hackathon.config;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class GCPCredentialsConfig {

    private final DocumentAiProperties properties;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {

        ClassPathResource resource =
                new ClassPathResource(properties.getCredentials());

        try (InputStream inputStream = resource.getInputStream()) {

            return GoogleCredentials.fromStream(inputStream)
                    .createScoped(Collections.singletonList(
                            "https://www.googleapis.com/auth/cloud-platform"));
        }
    }
}
