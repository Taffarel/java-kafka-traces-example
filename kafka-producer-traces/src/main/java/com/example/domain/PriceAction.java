package com.example.domain;

import java.time.LocalDateTime;

public interface PriceAction {
    double getOpen();
    double getClose();
    double getHigh();
    double getLow();
    int getVolume();
    LocalDateTime getDate();
    String getUnderlying();
}