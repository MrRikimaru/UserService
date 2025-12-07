package com.example.userservice.controller;

import com.example.userservice.service.CacheService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

  private final CacheService cacheService;

  @GetMapping("/stats")
  public ResponseEntity<Map<String, Object>> getCacheStats() {
    log.info("Getting cache statistics");

    CacheService.CacheStats stats = cacheService.getCacheStats();

    Map<String, Object> response = new HashMap<>();
    response.put("userCacheKeys", stats.getUserCacheKeys().size());
    response.put("userWithCardsCacheKeys", stats.getUserWithCardsCacheKeys().size());
    response.put("userCardsCacheKeys", stats.getUserCardsCacheKeys().size());
    response.put("totalKeys", stats.getTotalKeys());

    Map<String, Object> detailed = new HashMap<>();
    detailed.put("userCacheKeys", stats.getUserCacheKeys());
    detailed.put("userWithCardsCacheKeys", stats.getUserWithCardsCacheKeys());
    detailed.put("userCardsCacheKeys", stats.getUserCardsCacheKeys());
    response.put("details", detailed);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/clear/user/{userId}")
  public ResponseEntity<Map<String, String>> clearUserCache(@PathVariable Long userId) {
    log.info("Clearing cache for user ID: {}", userId);
    cacheService.evictUserCaches(userId);

    Map<String, String> response = new HashMap<>();
    response.put("message", "Cache cleared for user: " + userId);
    response.put("userId", userId.toString());

    return ResponseEntity.ok(response);
  }

  @PostMapping("/clear/all")
  public ResponseEntity<Map<String, String>> clearAllCache() {
    log.info("Clearing ALL cache");
    cacheService.evictAllUserCaches();

    Map<String, String> response = new HashMap<>();
    response.put("message", "All cache cleared");

    return ResponseEntity.ok(response);
  }

  @GetMapping("/log")
  public ResponseEntity<Map<String, String>> logCacheState() {
    log.info("Logging current cache state");
    cacheService.logCurrentCacheState();

    Map<String, String> response = new HashMap<>();
    response.put("message", "Cache state logged (check application logs)");

    return ResponseEntity.ok(response);
  }
}
