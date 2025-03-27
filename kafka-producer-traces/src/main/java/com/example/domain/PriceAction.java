package com.example.domain;

import java.time.LocalDateTime;

public interface PriceAction {
    double getOpen();
    double getClose();
    double getHigh();
    double getLow();
    LocalDateTime getDate();
    String getUnderlying();
}