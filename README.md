# resiliency
Resiliency implemented using core java without any external library.


# 💳 Resilient Payment Service (Java + Virtual Threads)

## 📌 Overview

This project demonstrates a **production-grade resilient payment API** built using:

* Java (Virtual Threads)
* Spring Boot
* Custom implementation (no external libraries)

It showcases how real-world systems handle failures using:

* ✅ Retry Mechanism
* ✅ Timeout Handling
* ✅ Circuit Breaker (with Sliding Window)
* ✅ Exponential Backoff
* ✅ Half-Open Recovery Strategy

---

## 🎯 Objective

To build a **fault-tolerant payment system** that can:

* Handle temporary failures gracefully
* Prevent cascading failures
* Recover automatically when dependencies become healthy

---

## 🏗️ High-Level Architecture

```
Client Request
      │
      ▼
Payment Controller
      │
      ▼
Payment Service
      │
      ▼
┌───────────────────────────────┐
│       Resiliency Layer        │
│                               │
│ 1. Circuit Breaker Check      │
│ 2. Retry with Backoff         │
│ 3. Timeout Handling           │
│ 4. Sliding Window Evaluation  │
└───────────────────────────────┘
      │
      ▼
External Payment API
```

---

## 🔄 Execution Flow

### ✅ Normal Flow

```
Request → External API → Success → Response Returned
```

---

### 🔁 Retry Flow

```
Request
   │
   ▼
Failure
   │
   ▼
Retry (with exponential delay)
   │
   ▼
Success OR Final Failure
```

---

### ⏱️ Timeout Flow

```
Request
   │
   ▼
External API (Delayed)
   │
   ▼
Timeout Triggered
   │
   ▼
Cancel Execution
   │
   ▼
Retry / Fail
```

---

### ⚡ Circuit Breaker Flow

```
                Failure Rate High
CLOSED  ─────────────────────────►  OPEN
  ▲                                    │
  │                                    │
  │                                    │
  └──────────── SUCCESS ◄──────── HALF-OPEN
```

---

## 🧠 Core Concepts

---

### 1️⃣ Retry Mechanism

Retries failed requests up to a configured limit.

#### Features:

* Max retry attempts configurable
* Exponential backoff delay

#### Example:

```
Attempt 1 → Fail
Attempt 2 → Retry after 1s
Attempt 3 → Retry after 2s
Attempt 4 → Retry after 4s
```

---

### 2️⃣ Timeout Handling

Ensures external API calls do not block indefinitely.

#### Implementation:

* Uses `Future.get(timeout)`
* Cancels task if timeout exceeded

#### Benefit:

* Prevents thread blocking
* Improves responsiveness

---

### 3️⃣ Circuit Breaker

Prevents system overload when external service is failing.

---

#### 🔹 States

##### 🟢 CLOSED

* Normal operation
* Requests pass through

##### 🔴 OPEN

* Triggered when failure threshold exceeded
* Requests are rejected immediately

##### 🟡 HALF-OPEN

* After cooldown period
* Allows **one test request**

---

### 4️⃣ Sliding Window (Advanced)

Instead of counting total failures, system tracks:

```
Last N requests → calculate failure %
```

#### Example:

```
Window Size = 10
Failures = 6

Failure Rate = 60% → Circuit Opens
```

---

### 5️⃣ Half-Open Control

Only **one request is allowed** when transitioning from OPEN → HALF-OPEN.

#### Why?

To avoid:

```
Multiple requests hitting unstable system
```

---

### 6️⃣ Exponential Backoff

Retry delay increases exponentially:

```
Delay = 2^attempt * base_time
```

#### Example:

| Attempt | Delay |
| ------- | ----- |
| 1       | 1 sec |
| 2       | 2 sec |
| 3       | 4 sec |

---

## ⚙️ Configuration

| Property              | Description            |
| --------------------- | ---------------------- |
| MAX_RETRIES           | Maximum retry attempts |
| TIMEOUT_DURATION      | API timeout duration   |
| CIRCUIT_OPEN_DURATION | Time before HALF-OPEN  |
| MAX_FAILURE_THRESHOLD | Failure threshold      |
| WINDOW_SIZE           | Sliding window size    |

---

## 🔁 Detailed Flow Diagram

```
                ┌───────────────────────┐
                │   Incoming Request    │
                └──────────┬────────────┘
                           │
                           ▼
               ┌───────────────────────┐
               │ Circuit Breaker Check │
               └──────────┬────────────┘
                          │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
   Circuit OPEN                    Circuit CLOSED
         │                               │
         ▼                               ▼
 Reject Request                 Call External API
                                         │
                                         ▼
                                ┌────────────────┐
                                │ Timeout Guard  │
                                └───────┬────────┘
                                        │
                    ┌───────────────────┴───────────────────┐
                    │                                       │
                    ▼                                       ▼
               Success                                  Failure/Timeout
                    │                                       │
                    ▼                                       ▼
         Reset Failure Count                    Record Failure (Sliding Window)
                                                        │
                                                        ▼
                                             Retry with Backoff (if allowed)
                                                        │
                                                        ▼
                                            Final Success / Failure
```

---

## 🧵 Virtual Threads Usage

* Tomcat is configured to use **Virtual Threads**
* Each request runs on a lightweight thread

### Benefits:

* High concurrency (thousands of requests)
* No thread exhaustion
* Simplified synchronous code

---

## 🧪 Failure Scenarios Handled

| Scenario            | Handling        |
| ------------------- | --------------- |
| External API slow   | Timeout         |
| Temporary failure   | Retry           |
| Continuous failures | Circuit Breaker |
| System recovery     | Half-Open state |

---

## 📈 Production Relevance

This design is similar to:

* Netflix resiliency patterns
* Payment gateway architectures
* Microservices fault tolerance design

---

## 🚀 Future Enhancements

* Rate Limiter
* Bulkhead Isolation
* Metrics & Monitoring (Prometheus)
* Distributed Tracing
* Idempotency (for payments)

---

## 🏁 Conclusion

This project demonstrates how to build a **robust, fault-tolerant system** using:

* Simple Java constructs
* Clean architecture
* Industry-proven patterns

It is designed to be:

* ✅ Scalable
* ✅ Maintainable
* ✅ Production-ready (baseline)

---

## 📌 Key Takeaway

> Resiliency is not about avoiding failures —
> it’s about **handling failures gracefully and recovering quickly**.

---
