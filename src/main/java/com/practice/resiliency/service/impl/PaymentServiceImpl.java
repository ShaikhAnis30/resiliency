package com.practice.resiliency.service.impl;

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
public class PaymentServiceImpl implements PaymentService {

  private final ExecutorService executorService;

  private final AtomicInteger retryCount = new AtomicInteger(0);
  private final AtomicInteger failureCount = new AtomicInteger(0);
  private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
  private final AtomicLong lastFailureTime = new AtomicLong(System.currentTimeMillis());


  @Override
  public String processPayment() {
    log.info("-----PaymentServiceImpl class, processPayment method-----");
    log.info("$$$$$$$$$$ PaymentServiceImpl thread: {}", Thread.currentThread());
    //logic
//    try {
//      return executorService.submit(() -> {
//        log.info("Payment processing...");
//        log.info("-----Thread name: " + Thread.currentThread());
    return simulatePayment();
//      }).get();
//    } catch (Exception e) {
//      log.error("Error processing payment: " + e.getMessage(), e);
//    }
//    return "PAYMENT FAILED";

    /****
     * Executor Service is not needed as configured the Tomcat to run on virtual threads
     * So no need to submit the task to executor service, just call the method directly as it will run on virtual thread
     * There will be no benefit of using executor service as virtual threads are already lightweight
     * and can handle large number of concurrent tasks without blocking the main thread
     *
     * CompletableFuture with Executor Service will be useful if we want to run some tasks
     * asynchronously and do not want to block the main thread,
     * but in this case we are simulating a payment process which is a synchronous task
     * and we want to return the result immediately,
     * so using CompletableFuture with Executor Service will add unnecessary complexity and overhead
     */

  }

  private String simulatePayment() {

    if (circuitOpen.get()) {
      return handleCircuitOpenState();
    }

    try {
      throw new Exception("Simulated payment failure");

      //Now here we need ExecutorService to call the external API with timeout,
      // as we want to simulate the timeout scenario and handle it gracefully
//      String result = handleExternalCallWithTimeout(this::makeExternalApiCall);
//      if (result.equals(Constants.FAILURE)) {
//        log.warn("##### External API call timeout, payment process aborted.");
//        return PaymentStatus.PAYMENT_FAILURE.name();//I have terminated, but can be retried.
//      }
//      return PaymentStatus.PAYMENT_SUCCESS.name();
    } catch (TimeoutException timeoutException) {
      log.warn("##### External API call timeout, payment process aborted: "
          + timeoutException.getMessage());
      return PaymentStatus.PAYMENT_FAILURE.name();
    } catch (Exception ex) {
      //resiliency logic
      updateCircuitBreaker();
      return handlePaymentWithRetry();
    }
  }

  private String handlePaymentWithRetry() {
    try {
      Thread.sleep(Constants.SLEEP_DURATION);
      int count = retryCount.incrementAndGet();
      if (count <= Constants.MAX_RETRIES) {
        log.info("Retrying payment... Attempt: " + count);
        return simulatePayment();
      } else {
        failureCount.set((failureCount.intValue() + count) - 1);
        retryCount.set(0);
        log.error("Payment failed after 3 attempts.");
        log.info("-----Failure count updated: " + failureCount);
        return PaymentStatus.PAYMENT_FAILURE.name();
      }
    } catch (InterruptedException ex) {
      log.warn("Payment retry interrupted: " + ex.getMessage());
    }
    return PaymentStatus.PAYMENT_SUCCESS.name();
  }

  private String makeExternalApiCall() {
    log.info("Making external API call...");

    try {
      log.info("-----External API called, waiting for response");
      Thread.sleep(2000); // Simulate delay
    } catch (Exception ex) {
      log.error("Error calling external API: " + ex.getMessage(), ex);
      return Constants.FAILURE;
    }
    return Constants.SUCCESS;
  }

  private <T> T handleExternalCallWithTimeout(Callable<T> task)
      throws ExecutionException, InterruptedException, TimeoutException {
    Future<T> externalApiResponse = executorService.submit(task);
    return externalApiResponse.get(Constants.TIMEOUT_DURATION, TimeUnit.MILLISECONDS);
  }


  private void updateCircuitBreaker() {
    int failureCount = this.failureCount.intValue();
    if (failureCount >= Constants.MAX_FAILURE_THRESHOLD) {
      log.warn("##### Circuit breaker opened due to consecutive failures. Failure count: "
          + failureCount);
      circuitOpen.set(true);
      lastFailureTime.set(System.currentTimeMillis());
    }
  }

  private String handleCircuitOpenState() {
    long currentTime = System.currentTimeMillis();
    long timeSinceLastFailure = currentTime - this.lastFailureTime.get();
    if (timeSinceLastFailure > Constants.CIRCUIT_OPEN_DURATION) {
//      log.info(
//          "##### Circuit half-open, allowing a test request to check if the external API has recovered. Last failure was "
//              + timeSinceLastFailure + " ms ago.");
//      String result = simulatePayment();
//      if (result.equals(PaymentStatus.PAYMENT_FAILURE.name())) {
//        log.warn(
//            "##### Test request failed, keeping circuit open. Last failure was " + timeSinceLastFailure
//                + " ms ago.");
//        this.lastFailureTime.set(System.currentTimeMillis());
//      } else {
//        log.info(
//            "##### Test request succeeded, closing circuit and resetting failure count. Last failure was "
//                + timeSinceLastFailure + " ms ago.");
//        this.circuitOpen.set(false);
//        this.failureCount.set(0);
//      }
//      return result;
      log.info("Circuit HALF-OPEN. Testing external API.");

      circuitOpen.set(false);

      return simulatePayment();
    } else {
      log.warn(
          "##### Circuit is open, rejecting payment request. Last failure was " + timeSinceLastFailure
              + " ms ago.");
      return PaymentStatus.PAYMENT_FAILURE.name();
    }
  }

}
