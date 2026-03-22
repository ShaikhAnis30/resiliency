package com.practice.resiliency.enums;

public enum CircuitBreakerState {
  OPEN("OPEN"),
  HALF_OPEN("HALF_OPEN"),
  CLOSED("CLOSED");

  private String value;

  CircuitBreakerState(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
