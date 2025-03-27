package com.example.presentation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.domain.PriceAction;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

public class RollingAverageCalculator {
    private static final Logger logger = LoggerFactory.getLogger(RollingAverageCalculator.class);
    private final Tracer tracer;
    private final int windowSize;

    public RollingAverageCalculator(Tracer tracer, int windowSize) {
        this.tracer = tracer;
        this.windowSize = windowSize;
    }

    public double calculateRollingAverage(List<PriceAction> priceActions) {
        Span span = tracer.spanBuilder("calculate-rolling-average")
                .setAttribute("windowSize", windowSize)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            if (priceActions.size() < windowSize) {
                return 0.0;
            }

            List<PriceAction> window = priceActions.subList(
                    priceActions.size() - windowSize, 
                    priceActions.size()
            );
            
            double sum = window.stream()
                    .mapToDouble(PriceAction::getClose)
                    .sum();
            
            return sum / windowSize;
        } finally {
            span.end();
        }
    }
}