package com.practice.resiliency.service.impl;

import com.practice.resiliency.enums.PaymentStatus;
import com.practice.resiliency.service.PaymentService;
import com.practice.resiliency.utils.Constants;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
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
public class PaymentServiceFullResiliencyImpl3 implements PaymentService {

  private final ExecutorService executorService;
  private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
  private final AtomicLong lastFailureTime = new AtomicLong(System.currentTimeMillis());
  private static final int WINDOW_SIZE = 10;
  private final Queue<Boolean> slidingWindow = new ConcurrentLinkedQueue<>();
  private final AtomicInteger failureInWindow = new AtomicInteger(0);
  private final AtomicBoolean halfOpenInProgress = new AtomicBoolean(false);


  @Override
  public String processPayment() {
    log.info("-----PaymentServiceImpl class, processPayment method-----");
    log.info("$$$$$$$$$$ PaymentServiceImpl thread: {}", Thread.currentThread());
    return simulatePaymentWithRetry(0);
  }

  private void recordResult(boolean success) {

    slidingWindow.add(success);

    if (!success) {
      failureInWindow.incrementAndGet();
    }

    if (slidingWindow.size() > WINDOW_SIZE) {
      Boolean removed = slidingWindow.poll();

      if (removed != null && !removed) {
        failureInWindow.decrementAndGet();
      }
    }

    evaluateCircuitState();
  }

  private void evaluateCircuitState() {

    if (slidingWindow.size() < WINDOW_SIZE) {
      return;
    }

    double failureRate =
        (failureInWindow.get() * 100.0) / WINDOW_SIZE;

    if (failureRate >= 50) { // threshold
      circuitOpen.set(true);
      lastFailureTime.set(System.currentTimeMillis());

      log.error("Circuit OPEN (failure rate: {}%)", failureRate);
    }
  }


  private String simulatePaymentWithRetry(int attempt) {

    if (circuitOpen.get()) {
      return handleCircuitOpenState();
    }

    try {

      String result = handleExternalCallWithTimeout(this::makeExternalApiCall);
      log.info("External API call result: " + result);

      recordResult(true);

      return PaymentStatus.PAYMENT_SUCCESS.name();

    } catch (Exception ex) {

      recordResult(false);

      if (attempt < Constants.MAX_RETRIES) {
        sleepWithBackOff(attempt);
        return simulatePaymentWithRetry(attempt + 1);
      }

      return PaymentStatus.PAYMENT_FAILURE.name();
    }
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

  private String handleCircuitOpenState() {

    long timeSinceLastFailure =
        System.currentTimeMillis() - lastFailureTime.get();

    if (timeSinceLastFailure > Constants.CIRCUIT_OPEN_DURATION) {

      if (halfOpenInProgress.compareAndSet(false, true)) {

        log.info("Circuit HALF-OPEN → allowing single test request");

        return testHalfOpen();

      } else {
        log.warn("HALF-OPEN in progress → rejecting request");
        return PaymentStatus.PAYMENT_FAILURE.name();
      }
    }

    log.warn("Circuit OPEN → rejecting request");
    return PaymentStatus.PAYMENT_FAILURE.name();
  }


  private String testHalfOpen() {

    try {

      String result = handleExternalCallWithTimeout(this::makeExternalApiCall);
      log.info("HALF-OPEN test result: " + result);

      // SUCCESS → close circuit
      circuitOpen.set(false);
      failureInWindow.set(0);
      slidingWindow.clear();

      log.info("Circuit CLOSED after successful test");

      return PaymentStatus.PAYMENT_SUCCESS.name();

    } catch (Exception ex) {

      // FAILURE → open again
      lastFailureTime.set(System.currentTimeMillis());

      log.warn("HALF-OPEN test failed → reopening circuit");

      return PaymentStatus.PAYMENT_FAILURE.name();

    } finally {
      halfOpenInProgress.set(false);
    }
  }
}
