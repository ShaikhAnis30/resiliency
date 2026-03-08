package com.practice.resiliency.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

public interface PaymentController {

  @PostMapping("/process")
  ResponseEntity<String> processPayment();

}
