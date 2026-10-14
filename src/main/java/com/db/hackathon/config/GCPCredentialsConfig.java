package com.db.hackathon.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Configuration
public class GCPCredentialsConfig {

    @Value("${google.credentials}")
    private String credentials;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {

        ClassPathResource resource =
                new ClassPathResource(credentials);

        try (InputStream inputStream = resource.getInputStream()) {

            return GoogleCredentials.fromStream(inputStream)
                    .createScoped(Collections.singletonList(
                            "https://www.googleapis.com/auth/cloud-platform"));
        }
    }
}
