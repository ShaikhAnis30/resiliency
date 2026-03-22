# resiliency
Resiliency implemented using core java without any external library.


# Circuit Breaker flow

Request
│
▼
Check Circuit
│
├─ OPEN → reject
│
▼
Call External API
│
├─ Success → reset failure count
│
└─ Failure → increment failure count
│
▼
if threshold reached
│
▼
open circuit



# Full Resiliency Stack (Production Systems)
Rate Limiter
│
▼
Bulkhead
│
▼
Timeout
│
▼
Retry
│
▼
Circuit Breaker
│
▼
Fallback

# Companies like:

# Netflix
# Stripe
# Amazon
# Uber

# use these patterns.
