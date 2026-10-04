-- Initialize the anomaly detection database

-- Anomalies table
CREATE TABLE IF NOT EXISTS anomalies (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    anomaly_type VARCHAR(20) NOT NULL,
    z_score DOUBLE PRECISION NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    volume DOUBLE PRECISION NOT NULL,
    raw_tick_data JSONB,
    ai_explanation TEXT,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    embedding DOUBLE PRECISION[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for querying by ticker and time
CREATE INDEX IF NOT EXISTS idx_anomalies_ticker_timestamp 
    ON anomalies(ticker, timestamp DESC);

-- Index for recent anomalies
CREATE INDEX IF NOT EXISTS idx_anomalies_timestamp 
    ON anomalies(timestamp DESC);

-- Tick data history (optional, for analysis)
CREATE TABLE IF NOT EXISTS tick_history (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    volume DOUBLE PRECISION NOT NULL,
    price_change DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for tick history queries
CREATE INDEX IF NOT EXISTS idx_tick_history_ticker_timestamp 
    ON tick_history(ticker, timestamp DESC);

-- Partitioning hint: In production, consider partitioning tick_history by time
