package com.anomalydetect.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * A detected anomalous market event.
 */
public class Anomaly {

    private Long id;
    private String ticker;
    private AnomalyType anomalyType;
    private double zScore;
    private double price;
    private double volume;
    private String rawTickData;
    private Instant timestamp;
    private double[] embedding;

    public Anomaly() {
    }

    public Anomaly(Long id, String ticker, AnomalyType anomalyType, double zScore,
                   double price, double volume, String rawTickData, Instant timestamp,
                   double[] embedding) {
        this.id = id;
        this.ticker = ticker;
        this.anomalyType = anomalyType;
        this.zScore = zScore;
        this.price = price;
        this.volume = volume;
        this.rawTickData = rawTickData;
        this.timestamp = timestamp;
        this.embedding = embedding;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public AnomalyType getAnomalyType() {
        return anomalyType;
    }

    public void setAnomalyType(AnomalyType anomalyType) {
        this.anomalyType = anomalyType;
    }

    public double getZScore() {
        return zScore;
    }

    public void setZScore(double zScore) {
        this.zScore = zScore;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public String getRawTickData() {
        return rawTickData;
    }

    public void setRawTickData(String rawTickData) {
        this.rawTickData = rawTickData;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public double[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Anomaly anomaly = (Anomaly) o;
        return Double.compare(zScore, anomaly.zScore) == 0
                && Double.compare(price, anomaly.price) == 0
                && Double.compare(volume, anomaly.volume) == 0
                && Objects.equals(id, anomaly.id)
                && Objects.equals(ticker, anomaly.ticker)
                && anomalyType == anomaly.anomalyType
                && Objects.equals(rawTickData, anomaly.rawTickData)
                && Objects.equals(timestamp, anomaly.timestamp)
                && Arrays.equals(embedding, anomaly.embedding);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, ticker, anomalyType, zScore, price, volume, rawTickData, timestamp);
        result = 31 * result + Arrays.hashCode(embedding);
        return result;
    }

    @Override
    public String toString() {
        return "Anomaly{" +
                "id=" + id +
                ", ticker='" + ticker + '\'' +
                ", anomalyType=" + anomalyType +
                ", zScore=" + zScore +
                ", price=" + price +
                ", volume=" + volume +
                ", timestamp=" + timestamp +
                '}';
    }
}
