package com.example.infrastructure;

import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

public class KafkaConsumerConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);
    private final Tracer tracer;

    public KafkaConsumerConfig(Tracer tracer) {
        this.tracer = tracer;
    }

    public KafkaConsumer<String, String> createConsumer() {
        Span span = tracer.spanBuilder("create-kafka-consumer").startSpan();
        try (Scope scope = span.makeCurrent()) {
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "application-kafka-bootstrap.kafka:9092");
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "price-action-group");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            return new KafkaConsumer<>(props);
        } finally {
            span.end();
        }
    }
}