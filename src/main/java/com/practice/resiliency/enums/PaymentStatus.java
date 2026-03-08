package com.practice.resiliency.enums;

public enum PaymentStatus {

  PAYMENT_SUCCESS("PAYMENT SUCCESS"),
  PAYMENT_FAILURE("PAYMENT FAILURE"),
  PENDING("PENDING");

  private String value;

  PaymentStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
