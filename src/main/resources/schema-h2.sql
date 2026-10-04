-- H2 compatible schema for local development

-- Anomalies table
CREATE TABLE IF NOT EXISTS anomalies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    anomaly_type VARCHAR(20) NOT NULL,
    z_score DOUBLE NOT NULL,
    price DOUBLE NOT NULL,
    volume DOUBLE NOT NULL,
    raw_tick_data CLOB,
    ai_explanation CLOB,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for querying by ticker and time
CREATE INDEX IF NOT EXISTS idx_anomalies_ticker_timestamp 
    ON anomalies(ticker, timestamp DESC);

-- Index for recent anomalies
CREATE INDEX IF NOT EXISTS idx_anomalies_timestamp 
    ON anomalies(timestamp DESC);

-- Tick data history (optional, for analysis)
CREATE TABLE IF NOT EXISTS tick_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    price DOUBLE NOT NULL,
    volume DOUBLE NOT NULL,
    price_change DOUBLE NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for tick history queries
CREATE INDEX IF NOT EXISTS idx_tick_history_ticker_timestamp 
    ON tick_history(ticker, timestamp DESC);
