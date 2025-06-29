package com.example.domain;

import java.time.LocalDateTime;

public class PriceActionImpl implements PriceAction {
    private final double open;
    private final double close;
    private final double high;
    private final double low;
    private final int volume;
    private final LocalDateTime date;
    private final String underlying;

    public PriceActionImpl(double open, double close, double high, double low, 
                            int volume,LocalDateTime date, String underlying) {
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
        this.date = date;
        this.underlying = underlying;
    }

    @Override public double getOpen() { return open; }
    @Override public double getClose() { return close; }
    @Override public double getHigh() { return high; }
    @Override public double getLow() { return low; }
    @Override public int getVolume() { return volume; }
    @Override public LocalDateTime getDate() { return date; }
    @Override public String getUnderlying() { return underlying; }
}