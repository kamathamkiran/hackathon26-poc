package com.db.hackathon.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class PubSubListener {
    @Value("${google.pubsub.subscription}")
    private String subscription;

    @Bean
    public MessageChannel agreementInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter inboundChannelAdapter(
            PubSubTemplate pubSubTemplate,
            MessageChannel agreementInputChannel
    ) {
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, subscription);

        adapter.setOutputChannel(agreementInputChannel);

        return adapter;
    }
}
