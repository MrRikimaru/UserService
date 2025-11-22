package com.example.userservice.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "UP", "service", "UserService"));
  }

  @GetMapping("/")
  public ResponseEntity<Map<String, String>> home() {
    return ResponseEntity.ok(Map.of("message", "User Service is running"));
  }
}
