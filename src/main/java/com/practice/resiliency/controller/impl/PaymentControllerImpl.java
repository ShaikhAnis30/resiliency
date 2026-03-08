package com.practice.resiliency.controller.impl;

import com.practice.resiliency.controller.PaymentController;
import com.practice.resiliency.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
public class PaymentControllerImpl implements PaymentController {

  private final PaymentService paymentService;

  @Override
  public ResponseEntity<String> processPayment() {
    log.info("-----PaymentControllerImpl class, processPayment method-----");
    log.info("$$$$$$$$$$ PaymentControllerImpl thread: {}", Thread.currentThread());
    return ResponseEntity.ok(paymentService.processPayment());
  }
}
