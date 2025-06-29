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
    private final String[] underlyings = {"AAPL", "TSLA", "TLT", "VIX", "RUT", "NVDA"};

    public RandomPriceGenerator(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public PriceAction generateRandomPriceAction() {
        Span span = tracer.spanBuilder("infrastructure.generate-price-action")
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            String underlying = underlyings[random.nextInt(underlyings.length)];
            
            // Define price ranges for each underlying
            double minPrice, maxPrice;
            switch (underlying) {
                case "AAPL":
                    minPrice = 160.0;
                    maxPrice = 260.0;
                    break;
                case "TSLA":
                    minPrice = 250.0;
                    maxPrice = 400.0;
                    break;
                case "TLT":
                    minPrice = 80.0;
                    maxPrice = 95.0;
                    break;
                case "VIX":
                    minPrice = 14.0;
                    maxPrice = 28.0;
                    break;
                case "RUT":
                    minPrice = 1750.0;
                    maxPrice = 2100.0;
                    break;
                case "NVDA":
                    minPrice = 100.0;
                    maxPrice = 200.0;
                    break;
                default:
                    minPrice = 100.0;
                    maxPrice = 1100.0; // Default range for other underlyings
                    break;
            }
    
            double basePrice = minPrice + (random.nextDouble() * (maxPrice - minPrice));
            double open = basePrice + (random.nextDouble() * 10);
            double high = open + (random.nextDouble() * 5);
            double low = Math.max(minPrice, open - (random.nextDouble() * 5)); // Ensure low doesn't go below minPrice
            double close = low + (random.nextDouble() * (high - low));
            int volume = Math.abs(random.nextInt() % 1000) * 1000; // Ensure non-negative volume
            
            PriceAction priceAction = new PriceActionImpl(
                open, close, high, low, volume, LocalDateTime.now(), underlying
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