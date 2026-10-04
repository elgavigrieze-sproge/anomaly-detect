# 📈 Anomaly Detector

Real-time stock market anomaly detection system with AI-powered explanations. Built with Spring Boot, Kafka, and Cloudflare Workers AI.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.5-blue)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| **Real-time Detection** | Z-score based anomaly detection on streaming tick data |
| **Kafka Event Streaming** | Scalable event-driven architecture with Apache Kafka |
| **AI Explanations** | LLM-generated explanations for detected anomalies |
| **Live Dashboard** | WebSocket-powered real-time alert visualization |
| **Multi-ticker Support** | Monitor stocks and crypto simultaneously |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DATA FLOW                                       │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────┐      ┌──────────────────┐      ┌──────────────────────┐
│  Data Simulator  │      │   Kafka Topic    │      │   Anomaly Detector   │
│  ─────────────── │      │   ───────────    │      │   ────────────────   │
│  • Generates     │ ───▶ │   stock-ticks    │ ───▶ │  • Consumes ticks    │
│    realistic     │      │                  │      │  • Z-score analysis  │
│    tick data     │      │                  │      │  • Detects anomalies │
│  • 9 tickers     │      │                  │      │                      │
│  • ~2 ticks/sec  │      └──────────────────┘      └──────────┬───────────┘
└──────────────────┘                                           │
                                                               │ Anomaly detected
                                                               ▼
┌──────────────────┐      ┌──────────────────┐      ┌──────────────────────┐
│   Web Browser    │      │    WebSocket     │      │   AI Explainer       │
│   ───────────    │      │    ─────────     │      │   ────────────       │
│  • Live alerts   │ ◀─── │  /ws/anomalies   │ ◀─── │  • Cloudflare AI     │
│  • Stats         │      │                  │      │  • Llama 3.3 70B     │
│  • Explanations  │      │                  │      │  • Human-readable    │
└──────────────────┘      └──────────────────┘      │    explanations      │
                                                    └──────────────────────┘
                                    │
                                    ▼
                          ┌──────────────────┐
                          │   Kafka Topic    │
                          │   ───────────    │
                          │   anomalies      │
                          │  (downstream)    │
                          └──────────────────┘
```

---

## 🛠️ Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Backend** | Java 21, Spring Boot 3.2, Spring WebFlux |
| **Messaging** | Apache Kafka (KRaft mode) |
| **AI/ML** | Cloudflare Workers AI (Llama 3.3 70B), Z-score statistical analysis |
| **Database** | PostgreSQL 16, R2DBC (reactive) |
| **Caching** | Redis 7 |
| **Frontend** | Vanilla JS, WebSocket, CSS3 |
| **Infrastructure** | Docker Compose |

---

## 🚀 Quick Start

Choose your deployment mode:

| Mode | Best For | Requirements |
|------|----------|--------------|
| **Local (no Docker)** | Development, laptops without Docker | Java 21, Maven |
| **Docker Compose** | Production-like, CI/CD | Docker, Java 21, Maven |

---

### Option A: Local Development (No Docker Required)

Runs everything in-memory — Kafka, Redis, and H2 database are embedded in the JVM. **No containers needed.**

#### Prerequisites
- Java 21
- Maven 3.8+

#### 1. Start the Anomaly Detector

```bash
cd anomaly-detect

# Start with embedded infrastructure (Kafka, Redis, H2)
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Wait for: `Started Application in X seconds`

#### 2. Start the Data Simulator

Open a **new terminal**:

```bash
cd anomaly-detect/data-simulator

# Option A: Simulated data (works offline)
mvn spring-boot:run

# Option B: Real market data from Finnhub (requires internet)
# Get a free API key from https://finnhub.io
# Windows PowerShell:
$env:FINNHUB_API_KEY="your_api_key_here"
mvn spring-boot:run "-Dspring-boot.run.arguments=--finnhub.enabled=true"

# Linux/Mac:
export FINNHUB_API_KEY=your_api_key_here
mvn spring-boot:run -Dspring-boot.run.arguments="--finnhub.enabled=true"
```

#### 3. View the Dashboard

Open **http://localhost:8080** in your browser.

Anomalies will start appearing after ~15 seconds (once baseline data is collected).

---

### Option B: Docker Compose (Production-like)

Uses real Kafka, PostgreSQL, and Redis containers.

#### Prerequisites
- Docker & Docker Compose
- Java 21
- Maven 3.8+

#### 1. Start Infrastructure

```bash
cd anomaly-detect
docker-compose up -d
```

This starts:
- **Kafka** on port 9092
- **Kafka UI** on port 8090
- **PostgreSQL** on port 5432
- **Redis** on port 6379

#### 2. Start the Anomaly Detector

```bash
mvn spring-boot:run
```

The detector starts on `http://localhost:8080`

#### 3. Start the Data Simulator

Open a **new terminal**:

```bash
cd anomaly-detect/data-simulator

# Simulated data
mvn spring-boot:run

# Or with real Finnhub data (Windows PowerShell)
$env:FINNHUB_API_KEY="your_api_key_here"
mvn spring-boot:run "-Dspring-boot.run.arguments=--finnhub.enabled=true"

# Or with real Finnhub data (Linux/Mac)
export FINNHUB_API_KEY=your_api_key_here
mvn spring-boot:run -Dspring-boot.run.arguments="--finnhub.enabled=true"
```

#### 4. View the Dashboard

Open `http://localhost:8080` in your browser.

---

### Using Real Market Data (Finnhub)

1. Sign up at [finnhub.io](https://finnhub.io) (free)
2. Copy your API key from the dashboard
3. Run the data simulator with Finnhub enabled:

**Windows (PowerShell):**
```powershell
$env:FINNHUB_API_KEY="your_api_key_here"
mvn spring-boot:run "-Dspring-boot.run.arguments=--finnhub.enabled=true"
```

**Windows (PowerShell) — if behind corporate proxy:**
```powershell
$env:FINNHUB_API_KEY="your_api_key_here"
$env:JAVA_TOOL_OPTIONS="-Dhttp.proxyHost= -Dhttps.proxyHost="
mvn spring-boot:run "-Dspring-boot.run.arguments=--finnhub.enabled=true"
```

**Linux/Mac:**
```bash
export FINNHUB_API_KEY=your_api_key_here
mvn spring-boot:run -Dspring-boot.run.arguments="--finnhub.enabled=true"
```

> **Note:** Finnhub's free tier provides real-time data from IEX exchange. You'll see live trades during US market hours (9:30 AM - 4:00 PM ET).

---

## 📊 Monitoring

| URL | Description |
|-----|-------------|
| `http://localhost:8080` | Live anomaly dashboard |
| `http://localhost:8090` | Kafka UI (topic inspection) |

---

## ⚙️ Configuration

### Environment Variables

```bash
# Cloudflare AI (optional - enables AI explanations)
export CLOUDFLARE_ACCOUNT_ID=your_account_id
export CLOUDFLARE_API_TOKEN=your_api_token

# Finnhub (optional - enables real market data)
export FINNHUB_API_KEY=your_finnhub_api_key
```

### Key Configuration (application.yml)

```yaml
anomaly:
  detection:
    z-score-threshold: 2.5      # Standard deviations to trigger alert
    rolling-window-size: 100     # Ticks for baseline calculation
    min-data-points: 30          # Minimum samples before detection
```

---

## 🔬 How Detection Works

### Z-Score Anomaly Detection

1. **Baseline Calculation**: For each ticker, maintain a rolling window of recent ticks
2. **Statistical Analysis**: Calculate mean (μ) and standard deviation (σ) for price changes and volume
3. **Z-Score Computation**: `z = (x - μ) / σ`
4. **Anomaly Flagging**: If |z| > threshold (default 2.5), flag as anomaly

### Severity Levels

| Z-Score | Severity | Meaning |
|---------|----------|---------|
| ≥ 4.0σ | CRITICAL | Extremely rare event (~0.006% probability) |
| ≥ 3.5σ | HIGH | Very unusual (~0.05% probability) |
| ≥ 3.0σ | MEDIUM | Notable deviation (~0.3% probability) |
| ≥ 2.5σ | LOW | Worth monitoring (~1.2% probability) |

---

## 📁 Project Structure

```
anomaly-detect/
├── docker-compose.yml          # Infrastructure (Kafka, Postgres, Redis)
├── init-db.sql                 # Database schema
├── pom.xml                     # Main service dependencies
├── src/main/
│   ├── java/com/anomalydetect/
│   │   ├── ai/                 # AI explanation service
│   │   │   └── AnomalyExplainerService.java
│   │   ├── buffer/             # Tick data buffering
│   │   ├── config/             # Configuration classes
│   │   ├── detection/          # Core anomaly detection
│   │   │   └── AnomalyDetector.java
│   │   ├── kafka/              # Kafka consumer/producer
│   │   │   ├── TickDataConsumer.java
│   │   │   └── AnomalyProducer.java
│   │   ├── model/              # Domain models
│   │   └── websocket/          # WebSocket handlers
│   └── resources/
│       ├── application.yml     # Configuration
│       └── static/index.html   # Dashboard UI
│
└── data-simulator/             # Separate tick data generator
    ├── pom.xml
    └── src/main/java/.../
        └── StockSimulator.java
```

---

## 🎯 Use Cases

- **Trading Platforms**: Real-time alerting for unusual market activity
- **Risk Management**: Early warning system for portfolio managers
- **Market Surveillance**: Detect potential manipulation or flash crashes
- **Algorithmic Trading**: Signal generation for trading strategies

---

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run with property-based testing
mvn test -Dtest=*PropertyTest
```

---

## 🔮 Future Enhancements

- [ ] Historical anomaly dashboard
- [ ] Slack/Discord notifications
- [ ] Machine learning models (LSTM, Isolation Forest)
- [ ] Multi-cluster Kafka support
- [ ] Kubernetes deployment manifests

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file.

---

## 👤 Author

**Elga Vigrieze**
- GitHub: [@ElgaVigrieze](https://github.com/ElgaVigrieze)
