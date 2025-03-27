package com.example;

import com.example.infrastructure.KafkaConsumerConfig;
import com.example.presentation.RollingAverageCalculator;
import com.example.service.PriceActionService;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public class App {
    public static void main(String[] args) {
        Tracer tracer = GlobalOpenTelemetry.getTracer("price-action-consumer");

        KafkaConsumerConfig kafkaConfig = new KafkaConsumerConfig(tracer);
        RollingAverageCalculator calculator = new RollingAverageCalculator(tracer, 5);
        PriceActionService service = new PriceActionService(kafkaConfig, calculator, tracer);

        service.startConsuming();
    }
}