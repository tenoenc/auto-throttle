# Auto Throttle

![Java](https://img.shields.io/badge/Java-21%2B-blue?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2%2B-green?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)

Auto Throttle is a lightweight, adaptive concurrency control library designed for Spring Boot applications. Unlike traditional rate limiters that require static configuration, Auto Throttle dynamically adjusts concurrency limits in real-time based on the server's response time (RTT), protecting the application from overload while maximizing throughput.

## Overview

In distributed systems, static rate limiting is often insufficient because the capacity of a service fluctuates depending on downstream dependencies, database performance, and garbage collection pauses.

Auto Throttle implements the **TCP Vegas congestion avoidance algorithm**, adapted for application-layer concurrency control. It treats the service as a network pipe, measuring the round-trip time of requests to detect queuing delay. When latency increases, it gracefully reduces the concurrency limit; when latency recovers, it explores higher limits.

### Key Features

* **Adaptive Control:** Automatically finds the optimal concurrency limit without manual tuning.
* **TCP Vegas Algorithm:** Uses queue size estimation based on minimum RTT vs. current RTT.
* **Zero-Overhead:** Built on Java 21 Virtual Threads and lock-free atomic primitives for nanosecond-level performance.
* **Fail-Fast:** Immediately rejects excess traffic with `HTTP 503 Service Unavailable` to prevent cascading failures.
* **Observability:** Seamless integration with Spring Boot Actuator and Micrometer.

## Requirements

* Java 21 or later
* Spring Boot 3.2 or later

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.yourid:auto-throttle-starter:1.0.0")
}
```

*Note: Replace `io.github.yourid` with your actual Group ID.*

## Usage

Once the dependency is added, Auto Throttle is active by default. It intercepts incoming HTTP requests using a Servlet Filter.

### Configuration

While the library is designed to work with zero configuration, you can tune the algorithm parameters in `application.yml` if necessary.

```yaml
auto-throttle:
  # Enable or disable the limiter (default: true)
  enabled: true
  
  # The time window for aggregating statistics (default: 100ms)
  window-size-ms: 100
  
  # TCP Vegas Alpha: The minimum expected queue size (default: 3)
  # If the estimated queue is smaller than this, the limit increases.
  alpha: 3
  
  # TCP Vegas Beta: The maximum expected queue size (default: 6)
  # If the estimated queue is larger than this, the limit decreases.
  beta: 6

```

### Monitoring

Auto Throttle integrates with Spring Boot Actuator to provide real-time visibility.

**Actuator Endpoint:**
`GET /actuator/autothrottle`

Response:

```json
{
  "limit": 50,
  "inflight": 12
}

```

**Micrometer Metrics:**
If you use Prometheus or other monitoring systems, the following metrics are exposed:

* `auto.throttle.limit`: The current dynamic concurrency limit.
* `auto.throttle.inflight`: The number of requests currently being processed.

To enable these endpoints, ensure your `application.yml` includes:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "autothrottle, prometheus, health"

```

## How It Works

1. **Measurement:** The library measures the Round-Trip Time (RTT) of every request using a high-performance ring buffer.
2. **Aggregation:** Every 100ms (configurable), it calculates the average RTT and tracks the minimum RTT (minRTT) seen so far.
3. **Estimation:** It calculates the estimated queue size using Little's Law principles derived from TCP Vegas:
> QueueSize = CurrentLimit * (1 - minRTT / CurrentRTT)


4. **Adjustment:**
* If `QueueSize < Alpha`: The system is underutilized. Increase the limit.
* If `QueueSize > Beta`: The system is congested. Decrease the limit.
* Otherwise: Maintain the current limit.



## License

This project is licensed under the MIT License. See the [LICENSE](https://www.google.com/search?q=LICENSE) file for details.
