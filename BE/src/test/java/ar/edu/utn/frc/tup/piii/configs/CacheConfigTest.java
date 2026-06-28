package ar.edu.utn.frc.tup.piii.configs;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void shouldCreateCacheManager() {
        CacheManager cacheManager = cacheConfig.cacheManager();
        assertNotNull(cacheManager);
        assertInstanceOf(ConcurrentMapCacheManager.class, cacheManager);
    }

    @Test
    void shouldHaveCardsCacheName() {
        ConcurrentMapCacheManager cacheManager = (ConcurrentMapCacheManager) cacheConfig.cacheManager();
        assertTrue(cacheManager.getCacheNames().contains("cards"));
    }
}
