package com.example.userservice.service;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final CacheManager cacheManager;
  private static final String CACHE_KEY_PREFIX = "user-service:";

  public void evictUserCaches(Long userId) {
    try {
      cacheManager.getCache("users").evictIfPresent(userId);
      cacheManager.getCache("usersWithCards").evictIfPresent(userId);
      cacheManager.getCache("userCards").evictIfPresent(userId);
      log.debug("Cache evicted for user: {}", userId);
    } catch (Exception e) {
      log.error("Error evicting cache for user {}: {}", userId, e.getMessage());
    }
  }

  public void evictAllUserCaches() {
    try {
      Set<String> allKeys = new HashSet<>();

      scanKeys(CACHE_KEY_PREFIX + "users::*", allKeys);
      scanKeys(CACHE_KEY_PREFIX + "usersWithCards::*", allKeys);
      scanKeys(CACHE_KEY_PREFIX + "userCards::*", allKeys);

      if (!allKeys.isEmpty()) {
        redisTemplate.delete(allKeys);
      }
      log.debug("All user caches evicted ({} keys)", allKeys.size());
    } catch (Exception e) {
      log.error("Error evicting all user caches: {}", e.getMessage());
    }
  }

  private void scanKeys(String pattern, Set<String> keys) {
    try (Cursor<String> cursor =
        redisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
      while (cursor.hasNext()) {
        String key = cursor.next();
        if (key != null) {
          keys.add(key);
        }
      }
    }
  }
}
