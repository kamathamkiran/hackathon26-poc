package com.db.hackathon.config.services;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PubSubCredentialsConfig {

    private final GoogleCredentials googleCredentials;

    public PubSubCredentialsConfig(GoogleCredentials googleCredentials) {
        this.googleCredentials = googleCredentials;
    }

    @Bean
    public TransportChannelProvider transportChannelProvider() {
        return TopicAdminSettings.defaultGrpcTransportProviderBuilder().build();
    }

    @Bean
    public CredentialsProvider credentialsProvider() {
        return FixedCredentialsProvider.create(googleCredentials);
    }
}
