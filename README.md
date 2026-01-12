
---

# System Monitoring

A backend system monitoring platform designed to ingest metrics from clients, process them asynchronously, store historical data, stream real-time updates, and send notifications.


---

## System Overview

Enterprise-grade reactive microservices monitoring platform built with **Spring Boot**, **Spring WebFlux**, **Apache Kafka**, and **PostgreSQL**. The platform provides reactive REST APIs, **Server-Sent Events (SSE)** streaming for real-time metrics, Redis caching, Flyway migrations, and **Resilience4j** for fault tolerance.

Designed as scalable, containerized microservices using Docker, the system allows clients to send metrics which are processed asynchronously, stored in a reliable database, and streamed in real-time to dashboards and applications.  

The platform consists of multiple independent services, each responsible for a specific part of the monitoring pipeline. Services communicate via **event-driven pipelines (Kafka)** and **REST**, ensuring loose coupling, high availability, and responsiveness.  

Clients send metrics to the system and receive real-time updates using SSE, while the Notification Service pushes alerts to external channels.

![WhatsApp Image 2026-01-11 at 5 15 45 PM](https://github.com/user-attachments/assets/ee6a2b9d-b354-4dd1-bf18-b8b502981f79)

---

## High-Level Architecture

### Clients

* Web Application
* Dashboard UI
* Monitoring Agents

### Backend Services

* **Ingestion Service**
* **Processing & Alert Service**
* **Streaming Service**
* **Notification Service**

### Infrastructure Components

* Message Broker (Kafka or similar)
* PostgreSQL
* Redis (optional caching)

---

## Services Description

### 1. Ingestion Service

Responsible for receiving metrics from clients.

**Responsibilities:**

* Expose REST APIs for metric ingestion
* Validate and normalize incoming metrics
* Publish metric events to the message pipeline

**Example Endpoint:**

```
POST /api/metrics
```

---

### 2. Processing & Alert Service

Consumes metric events from the pipeline and processes them.

**Responsibilities:**

* Consume metric events from Kafka
* Apply aggregation and evaluation logic
* Persist metrics to PostgreSQL
* Publish processed metric events to downstream services (Streaming & Notification)

---

### 3. Streaming Service

Responsible for real-time data delivery to clients.

**Responsibilities:**

* Maintain SSE connections with clients
* Subscribe to processed metric events
* Stream real-time metric updates to connected clients

**Communication Pattern:**

* **Server-Sent Events (SSE)** from service → client

---

### 4. Notification Service

Responsible for sending alerts to external systems.

**Responsibilities:**

* Consume alert events from the processing pipeline
* Send notifications via:

  * Slack
  * Email
  * Webhooks
* Implement retries and circuit breakers for reliability

**Communication Pattern:**

* **Consumes:** Processed alert events from Kafka
* **External Integration:** Slack, Email, Webhook

---

## Data Flow (CPU Metric Example)

1. Client sends CPU usage metric to **Ingestion Service** via HTTP
2. Ingestion Service publishes a `MetricEvent` to the message broker
3. Processing Service consumes the event
4. Processing Service stores metric data in PostgreSQL
5. Processing Service publishes processed events to downstream services:

   * **Streaming Service** → pushes real-time metrics to clients via SSE
   * **Notification Service** → sends alerts to external channels

---

## Communication Patterns

| Interaction             | Protocol               |
| ----------------------- | ---------------------- |
| Client → Ingestion      | HTTP REST              |
| Service → Service       | Event Pipeline (Kafka) |
| Streaming → Client      | SSE                    |
| Notification → External | HTTP / Web API         |
| Data Persistence        | PostgreSQL             |

---

## Technology Stack

* **Java 17**
* **Spring Boot**
* **Spring Web / WebFlux**
* **Kafka (or Message Broker)**
* **PostgreSQL**
* **Redis**
* **Docker**

---

## Project Structure (Multi-Module)

```
system-monitoring/
│
├── docker-compose.yml                    
├── pom.xml                              
│
├── common-lib/                         
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/
│             └── com/monitoring/common/
│ 
├── ingestion-service/                   
│   ├── Dockerfile                       
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/monitoring/ingestion/
│       │   └── resources/
│       │       └── application.yml
│       └── test/
│
├── processing-alert-service/          
│   ├── Dockerfile                      
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/monitoring/processing/
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       │           ├── V1__create_metrics_table.sql
│       │           ├── V2__create_alerts_table.sql
│       │           └── V3__create_alert_rules_table.sql
│       └── test/
│
├── streaming-service/                 
│   ├── Dockerfile                       
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/monitoring/streaming/
│       │   └── resources/
│       │       └── application.yml
│       └── test/
│
└── notification-service/                
    ├── Dockerfile                    
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── com/monitoring/notification/
        │   └── resources/
        │       ├── application.yml
        │       └── templates/
        │           └── alert-notification.html
        └── test/
```

Each service is an independent deployable unit with its own configuration and lifecycle.

---

## Key Characteristics

* Event-driven architecture
* Loose coupling between services
* Real-time streaming via SSE
* External notifications via Slack, Email, Webhook
* Horizontally scalable services
* Clear separation of responsibilities

---
