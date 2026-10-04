package com.anomalydetect.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * A detected anomalous market event.
 */

@RequiredArgsConstructor
@Data
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