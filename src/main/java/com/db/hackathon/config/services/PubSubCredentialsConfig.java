package com.db.hackathon.config.services;

import com.db.hackathon.config.DocumentAiProperties;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PubSubCredentialsConfig {

    private final GoogleCredentials googleCredentials;
    private final DocumentAiProperties properties;

    @Bean
    public TransportChannelProvider transportChannelProvider() {
        return TopicAdminSettings.defaultGrpcTransportProviderBuilder().build();
    }

    @Bean
    public CredentialsProvider credentialsProvider() {
        return FixedCredentialsProvider.create(googleCredentials);
    }
}
