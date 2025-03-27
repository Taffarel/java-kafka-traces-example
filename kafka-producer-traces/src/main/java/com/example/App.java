package com.example;

import com.example.infrastructure.KafkaProducerClient;
import com.example.infrastructure.RandomPriceGenerator;
import com.example.service.PriceActionService;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public class App {
    public static void main(String[] args) {
        Tracer tracer = GlobalOpenTelemetry.getTracer("price-action-producer");

        // Initialize components
        RandomPriceGenerator priceGenerator = new RandomPriceGenerator(tracer);
        KafkaProducerClient producerClient = new KafkaProducerClient(tracer);
        PriceActionService service = new PriceActionService(producerClient, priceGenerator, tracer);

        // Produce some price actions
        for (int i = 0; i < 10; i++) {
            service.producePriceAction();
            try {
                Thread.sleep(1000); // Wait 1 second between productions
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}