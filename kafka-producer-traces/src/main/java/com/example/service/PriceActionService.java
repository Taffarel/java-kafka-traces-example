package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.domain.PriceAction;
import com.example.infrastructure.KafkaProducerClient;
import com.example.infrastructure.RandomPriceGenerator;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

public class PriceActionService {
    private static final Logger logger = LoggerFactory.getLogger(PriceActionService.class);
    private final KafkaProducerClient producerClient;
    private final RandomPriceGenerator priceGenerator;
    private final Tracer tracer;

    public PriceActionService(KafkaProducerClient producerClient, 
                            RandomPriceGenerator priceGenerator,
                            Tracer tracer) {
        this.producerClient = producerClient;
        this.priceGenerator = priceGenerator;
        this.tracer = tracer;
    }

    public void producePriceAction() {
        Span span = tracer.spanBuilder("service.produce-price-action")
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            PriceAction priceAction = priceGenerator.generateRandomPriceAction();
            producerClient.sendPriceAction(priceAction);
            logger.info("Successfully produced price action for {}", 
                       priceAction.getUnderlying());
        } catch (Exception e) {
            span.recordException(e);
            logger.error("Error producing price action", e);
            throw e;
        } finally {
            span.end();
        }
    }
}