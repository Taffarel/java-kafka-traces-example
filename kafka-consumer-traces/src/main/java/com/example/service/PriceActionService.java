package com.example.service;

import com.example.domain.PriceAction;
import com.example.domain.PriceActionImpl;
import com.example.infrastructure.KafkaConsumerConfig;
import com.example.presentation.RollingAverageCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PriceActionService {
    private static final Logger logger = LoggerFactory.getLogger(PriceActionService.class);
    private final KafkaConsumerConfig kafkaConfig;
    private final RollingAverageCalculator rollingAverageCalculator;
    private final Tracer tracer;
    private final ObjectMapper objectMapper;
    private final List<PriceAction> priceActions = new ArrayList<>();
    private final W3CTraceContextPropagator propagator = W3CTraceContextPropagator.getInstance();

    private static final TextMapGetter<ConsumerRecord<String, String>> HEADER_GETTER =
        new TextMapGetter<ConsumerRecord<String, String>>() {
            @Override
            public Iterable<String> keys(ConsumerRecord<String, String> record) {
                List<String> keys = new ArrayList<>();
                for (Header header : record.headers()) {
                    keys.add(header.key());
                }
                return keys;
            }

            @Override
            public String get(ConsumerRecord<String, String> record, String key) {
                Header header = record.headers().lastHeader(key);
                return header != null ? new String(header.value()) : null;
            }
        };

    public PriceActionService(
            KafkaConsumerConfig kafkaConfig,
            RollingAverageCalculator rollingAverageCalculator,
            Tracer tracer) {
        this.kafkaConfig = kafkaConfig;
        this.rollingAverageCalculator = rollingAverageCalculator;
        this.tracer = tracer;
        this.objectMapper = configureObjectMapper();
    }

    private ObjectMapper configureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Custom deserializer for PriceActionImpl
        SimpleModule module = new SimpleModule();
        module.addDeserializer(PriceActionImpl.class, new com.fasterxml.jackson.databind.JsonDeserializer<PriceActionImpl>() {
            @Override
            public PriceActionImpl deserialize(com.fasterxml.jackson.core.JsonParser p, 
                                            com.fasterxml.jackson.databind.DeserializationContext ctxt) 
                    throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode node = p.getCodec().readTree(p);
                double open = node.get("open").asDouble();
                double close = node.get("close").asDouble();
                double high = node.get("high").asDouble();
                double low = node.get("low").asDouble();
                LocalDateTime date = LocalDateTime.parse(node.get("date").asText());
                String underlying = node.get("underlying").asText();

                return new PriceActionImpl(open, close, high, low, date, underlying);
            }
        });
        mapper.registerModule(module);

        return mapper;
    }

    public void startConsuming() {
        Span span = tracer.spanBuilder("start-consuming-price-actions").startSpan();
        try (Scope scope = span.makeCurrent()) {
            try (KafkaConsumer<String, String> consumer = kafkaConfig.createConsumer()) {
                consumer.subscribe(Collections.singletonList("price-action"));

                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        processRecord(record);
                    }
                }
            } catch (Exception e) {
                logger.error("Error in consumer loop", e);
                span.recordException(e);
                throw e;
            }
        } finally {
            span.end();
        }
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        // Extract context from Kafka headers
        Context extractedContext = propagator.extract(Context.current(), record, HEADER_GETTER);
        Span span = tracer.spanBuilder("process-price-action-record")
                .setParent(extractedContext)
                .setAttribute("offset", record.offset())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            PriceAction priceAction = objectMapper.readValue(record.value(), PriceActionImpl.class);
            priceActions.add(priceAction);
            
            double rollingAverage = rollingAverageCalculator.calculateRollingAverage(priceActions);
            logger.info("Processed price action. Rolling Average: {}", rollingAverage);
            
        } catch (Exception e) {
            logger.error("Error processing record", e);
            span.recordException(e);
        } finally {
            span.end();
        }
    }
}