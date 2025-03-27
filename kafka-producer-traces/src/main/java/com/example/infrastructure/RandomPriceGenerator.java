package com.example.infrastructure;

import java.time.LocalDateTime;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.domain.PriceAction;
import com.example.domain.PriceActionImpl;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

public class RandomPriceGenerator {
    private static final Logger logger = LoggerFactory.getLogger(RandomPriceGenerator.class);
    private final Random random = new Random();
    private final Tracer tracer;
    private final String[] underlyings = {"AAPL", "TSLA", "TLT", "VIX", "RUT"};

    public RandomPriceGenerator(Tracer tracer) {
        this.tracer = tracer;
    }

    public PriceAction generateRandomPriceAction() {
        Span span = tracer.spanBuilder("infrastructure.generate-price-action")
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            String underlying = underlyings[random.nextInt(underlyings.length)];
            double basePrice = 100 + (random.nextDouble() * 1000);
            double open = basePrice + (random.nextDouble() * 10);
            double high = open + (random.nextDouble() * 5);
            double low = open - (random.nextDouble() * 5);
            double close = low + (random.nextDouble() * (high - low));
            
            PriceAction priceAction = new PriceActionImpl(
                open, close, high, low, LocalDateTime.now(), underlying
            );
            
            span.setAttribute("underlying", underlying);
            span.setAttribute("price", close);
            logger.info("Generated price action for {}", underlying);
            return priceAction;
        } finally {
            span.end();
        }
    }
}