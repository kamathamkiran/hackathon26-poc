package com.db.hackathon.config.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    private final GoogleCredentials googleCredentials;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    public StorageConfig(GoogleCredentials googleCredentials) {
        this.googleCredentials = googleCredentials;
    }

    @Bean
    public Storage storage() {

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(googleCredentials)
                .build()
                .getService();
    }
}
