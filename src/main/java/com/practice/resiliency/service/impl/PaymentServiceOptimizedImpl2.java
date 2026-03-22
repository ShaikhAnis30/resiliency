package com.practice.resiliency.service.impl;

import com.practice.resiliency.enums.CircuitBreakerState;
import com.practice.resiliency.enums.PaymentStatus;
import com.practice.resiliency.service.PaymentService;
import com.practice.resiliency.utils.Constants;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentServiceOptimizedImpl2 implements PaymentService {

  private final ExecutorService executorService;
  private final AtomicInteger failureCount = new AtomicInteger(0);
  private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
  private final AtomicLong lastFailureTime = new AtomicLong(System.currentTimeMillis());


  @Override
  public String processPayment() {
    log.info("-----PaymentServiceImpl class, processPayment method-----");
    log.info("$$$$$$$$$$ PaymentServiceImpl thread: {}", Thread.currentThread());
    return simulatePaymentWithRetry(0);
  }

  private String simulatePaymentWithRetry(int attempt) {

    if (circuitOpen.get()) {
      return handleCircuitOpenState();
    }

    try {
      String result = handleExternalCallWithTimeout(this::makeExternalApiCall);

      log.info("External API call result: " + result);

      failureCount.set(0);

      return PaymentStatus.PAYMENT_SUCCESS.name();
    } catch (TimeoutException timeoutException) {

      log.warn("Timeout Occurred");
      updateCircuitBreaker();

    } catch (Exception e) {

      log.warn("Exception Occurred");
      updateCircuitBreaker();

    }

    //Retry Logic
    if (attempt < Constants.MAX_RETRIES) {

      sleepWithBackOff(attempt);

      return simulatePaymentWithRetry(attempt + 1);
    }

    return PaymentStatus.PAYMENT_FAILURE.name();
  }


  private void sleepWithBackOff(int attempt) {
    try {

      long delay = (long) Math.pow(2, attempt) * 1000;
      log.info("Retrying after " + delay + " ms...");
      Thread.sleep(delay);
    } catch (InterruptedException ex) {
      log.warn("Sleep interrupted: " + ex.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  private String makeExternalApiCall() {
    log.info("Making external API call...");

    try {
      log.info("-----External API called, waiting for response");
      //Some external API call logic here, which can potentially throw exceptions or timeout
    } catch (Exception ex) {
      log.error("Error calling external API: " + ex.getMessage(), ex);
      return Constants.FAILURE;
    }
    return Constants.SUCCESS;
  }

  private <T> T handleExternalCallWithTimeout(Callable<T> task)
      throws ExecutionException, InterruptedException, TimeoutException {
    Future<T> future = executorService.submit(task);
    try {
      return future.get(Constants.TIMEOUT_DURATION, TimeUnit.MILLISECONDS);
    } catch (TimeoutException timeoutException) {
      future.cancel(true);
      throw timeoutException;
    }
  }


  private void updateCircuitBreaker() {
    int failures = this.failureCount.incrementAndGet();
    log.warn("Failure recorded. Count: {}", failures);

    if (failures >= Constants.MAX_FAILURE_THRESHOLD) {

      circuitOpen.set(true);

      lastFailureTime.set(System.currentTimeMillis());

      log.warn("Circuit" + CircuitBreakerState.OPEN
          + " -> opened due to consecutive failures. Failure count: " + failures);
    }
  }

  private String handleCircuitOpenState() {
    long timeSinceLastFailure = System.currentTimeMillis() - this.lastFailureTime.get();

    if (timeSinceLastFailure >= Constants.CIRCUIT_OPEN_DURATION) {

      log.info("Circuit " + CircuitBreakerState.HALF_OPEN + " -> allowing test request");
      circuitOpen.set(false);

      // allow ONLY ONE request ideally
      return simulatePaymentWithRetry(0);
    }

    log.warn("Circuit " + CircuitBreakerState.OPEN + " -> rejecting requests");
    return PaymentStatus.PAYMENT_FAILURE.name();
  }

}
