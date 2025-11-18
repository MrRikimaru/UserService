package com.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void evictUserCaches(Long userId) {
        try {
            redisTemplate.delete("users::" + userId);
            redisTemplate.delete("usersWithCards::" + userId);
            redisTemplate.delete("userCards::" + userId);
            log.debug("Cache evicted for user: {}", userId);
        } catch (Exception e) {
            log.error("Error evicting cache for user {}: {}", userId, e.getMessage());
        }
    }

    public void evictAllUserCaches() {
        try {
            Set<String> userKeys = redisTemplate.keys("users*");
            Set<String> userWithCardsKeys = redisTemplate.keys("usersWithCards*");
            Set<String> userCardsKeys = redisTemplate.keys("userCards*");

            if (!userKeys.isEmpty()) {
                redisTemplate.delete(userKeys);
            }
            if (!userWithCardsKeys.isEmpty()) {
                redisTemplate.delete(userWithCardsKeys);
            }
            if (!userCardsKeys.isEmpty()) {
                redisTemplate.delete(userCardsKeys);
            }
            log.debug("All user caches evicted");
        } catch (Exception e) {
            log.error("Error evicting all user caches: {}", e.getMessage());
        }
    }
}