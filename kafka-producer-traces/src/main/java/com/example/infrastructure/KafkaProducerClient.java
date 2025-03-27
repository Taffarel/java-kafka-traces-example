package com.example.infrastructure;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.domain.PriceAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;

public class KafkaProducerClient {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerClient.class);
    private final KafkaProducer<String, String> producer;
    private final Tracer tracer;
    private final String topic = "price-action";
    private final ObjectMapper objectMapper = new ObjectMapper();

    // OpenTelemetry context propagator
    private final TextMapSetter<Headers> headerSetter = (headers, key, value) -> {
        if (headers != null && key != null && value != null) {
            headers.add(key, value.getBytes());
        }
    };

    public KafkaProducerClient(Tracer tracer) {
        this.tracer = tracer;
        Properties props = new Properties();
        String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") != null ?
            System.getenv("KAFKA_BOOTSTRAP_SERVERS") : "application-kafka-bootstrap.kafka:9092";
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.producer = new KafkaProducer<>(props);

        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void sendPriceAction(PriceAction priceAction){
        Span span = tracer.spanBuilder("infrastructure.send-price-action")
                .startSpan();
                
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("underlying", priceAction.getUnderlying());
            
            String jsonValue;
            try {
                jsonValue = objectMapper.writeValueAsString(priceAction);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                span.recordException(e);
                logger.error("Error serializing price action to JSON", e);
                throw e; // or handle it differently if needed
            }

            // Create record with headers
            Headers headers = new RecordHeaders();
            
            // Inject trace context into headers
            io.opentelemetry.context.Context.current()
                .with(span)
                .makeCurrent();

            io.opentelemetry.api.GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), headers, headerSetter);

            ProducerRecord<String, String> record = 
                new ProducerRecord<>(topic, null, priceAction.getUnderlying(), jsonValue, headers);
            
            producer.send(record);
            logger.info("Sent price action for {} with traceId: {}", 
                priceAction.getUnderlying(), 
                span.getSpanContext().getTraceId());
        } catch (Exception e) {
            span.recordException(e);
            logger.error("Error sending price action", e);
        } finally {
            span.end();
        }
    }
}