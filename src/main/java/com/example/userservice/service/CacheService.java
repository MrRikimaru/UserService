package com.example.userservice.service;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
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

  public void evictUserCaches(Long userId) {
    try {
      log.info("Evicting caches for user ID: {}", userId);

      Cache usersCache = cacheManager.getCache("users");
      Cache usersWithCardsCache = cacheManager.getCache("usersWithCards");
      Cache userCardsCache = cacheManager.getCache("userCards");

      boolean evicted = false;

      if (usersCache != null) {
        usersCache.evictIfPresent(userId);
        log.debug("Cache 'users' evicted for key: {}", userId);
        evicted = true;
      }

      if (usersWithCardsCache != null) {
        usersWithCardsCache.evictIfPresent(userId);
        log.debug("Cache 'usersWithCards' evicted for key: {}", userId);
        evicted = true;
      }

      if (userCardsCache != null) {
        userCardsCache.evictIfPresent(userId);
        log.debug("Cache 'userCards' evicted for key: {}", userId);
        evicted = true;
      }

      if (evicted) {
        log.info("Successfully evicted all caches for user: {}", userId);
      } else {
        log.warn("No caches found to evict for user: {}", userId);
      }

    } catch (Exception e) {
      log.error("Error evicting cache for user {}: {}", userId, e.getMessage(), e);
    }
  }

  public void evictAllUserCaches() {
    try {
      log.info("Starting to evict all user caches...");
      Set<String> allKeys = new HashSet<>();

      // Ищем все ключи с префиксом
      scanKeys("user-service:*", allKeys);

      if (!allKeys.isEmpty()) {
        redisTemplate.delete(allKeys);
        log.info("Evicted {} cache keys: {}", allKeys.size(), allKeys);
      } else {
        log.info("No cache keys found to evict");
      }

    } catch (Exception e) {
      log.error("Error evicting all user caches: {}", e.getMessage(), e);
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
    } catch (Exception e) {
      log.error("Error scanning keys with pattern {}: {}", pattern, e.getMessage());
    }
  }

  public CacheStats getCacheStats() {
    CacheStats stats = new CacheStats();

    try {
      Set<String> allKeys = new HashSet<>();
      // Ищем все ключи, начинающиеся с префикса
      scanKeys("user-service:*", allKeys);

      log.info("Found {} total keys in Redis: {}", allKeys.size(), allKeys);

      Set<String> userKeys = new HashSet<>();
      Set<String> userWithCardsKeys = new HashSet<>();
      Set<String> userCardsKeys = new HashSet<>();

      for (String key : allKeys) {
        log.debug("Analyzing key: {}", key);
        // Проверяем по частям
        if (key.contains("::users::") || key.contains(":users::")) {
          if (!key.contains("usersWithCards")) {
            userKeys.add(key);
            log.debug("Added to userKeys: {}", key);
          }
        } else if (key.contains("::usersWithCards::") || key.contains(":usersWithCards::")) {
          userWithCardsKeys.add(key);
          log.debug("Added to userWithCardsKeys: {}", key);
        } else if (key.contains("::userCards::") || key.contains(":userCards::")) {
          userCardsKeys.add(key);
          log.debug("Added to userCardsKeys: {}", key);
        } else {
          log.debug("Key doesn't match any known pattern: {}", key);
        }
      }

      stats.setUserCacheKeys(userKeys);
      stats.setUserWithCardsCacheKeys(userWithCardsKeys);
      stats.setUserCardsCacheKeys(userCardsKeys);

      log.info(
          "Cache stats collected: users={}, usersWithCards={}, userCards={}, total={}",
          userKeys.size(),
          userWithCardsKeys.size(),
          userCardsKeys.size(),
          allKeys.size());

    } catch (Exception e) {
      log.error("Error collecting cache stats: {}", e.getMessage(), e);
    }

    return stats;
  }

  public void logCurrentCacheState() {
    try {
      CacheStats stats = getCacheStats();

      log.info("=== CURRENT CACHE STATE ===");
      log.info(
          "Users cache keys ({}): {}", stats.getUserCacheKeys().size(), stats.getUserCacheKeys());
      log.info(
          "UsersWithCards cache keys ({}): {}",
          stats.getUserWithCardsCacheKeys().size(),
          stats.getUserWithCardsCacheKeys());
      log.info(
          "UserCards cache keys ({}): {}",
          stats.getUserCardsCacheKeys().size(),
          stats.getUserCardsCacheKeys());
      log.info(
          "=== TOTAL KEYS: {} ===",
          stats.getUserCacheKeys().size()
              + stats.getUserWithCardsCacheKeys().size()
              + stats.getUserCardsCacheKeys().size());

    } catch (Exception e) {
      log.error("Error logging cache state: {}", e.getMessage(), e);
    }
  }

  public static class CacheStats {
    private Set<String> userCacheKeys = new HashSet<>();
    private Set<String> userWithCardsCacheKeys = new HashSet<>();
    private Set<String> userCardsCacheKeys = new HashSet<>();

    public Set<String> getUserCacheKeys() {
      return userCacheKeys;
    }

    public void setUserCacheKeys(Set<String> userCacheKeys) {
      this.userCacheKeys = userCacheKeys;
    }

    public Set<String> getUserWithCardsCacheKeys() {
      return userWithCardsCacheKeys;
    }

    public void setUserWithCardsCacheKeys(Set<String> userWithCardsCacheKeys) {
      this.userWithCardsCacheKeys = userWithCardsCacheKeys;
    }

    public Set<String> getUserCardsCacheKeys() {
      return userCardsCacheKeys;
    }

    public void setUserCardsCacheKeys(Set<String> userCardsCacheKeys) {
      this.userCardsCacheKeys = userCardsCacheKeys;
    }

    public int getTotalKeys() {
      return userCacheKeys.size() + userWithCardsCacheKeys.size() + userCardsCacheKeys.size();
    }
  }
}
