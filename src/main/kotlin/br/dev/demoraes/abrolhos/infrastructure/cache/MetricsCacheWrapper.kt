package br.dev.demoraes.abrolhos.infrastructure.cache

import br.dev.demoraes.abrolhos.infrastructure.monitoring.MetricsService
import java.util.concurrent.Callable
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

class MetricsCacheManager(
        private val delegate: CacheManager,
        private val metricsService: MetricsService
) : CacheManager {

    override fun getCache(name: String): Cache? {
        val cache = delegate.getCache(name) ?: return null
        return MetricsCache(cache, metricsService)
    }

    override fun getCacheNames(): Collection<String> {
        return delegate.cacheNames
    }
}

class MetricsCache(private val delegate: Cache, private val metricsService: MetricsService) :
        Cache {

    override fun getName(): String = delegate.name

    override fun getNativeCache(): Any = delegate.nativeCache

    override fun get(key: Any): Cache.ValueWrapper? {
        val result = delegate.get(key)
        if (result == null) {
            metricsService.recordCacheMiss()
        } else {
            metricsService.recordCacheHit()
        }
        return result
    }

    override fun <T : Any?> get(key: Any, type: Class<T>?): T? {
        val result = delegate.get(key, type)
        if (result == null) {
            metricsService.recordCacheMiss()
        } else {
            metricsService.recordCacheHit()
        }
        return result
    }

    override fun <T : Any?> get(key: Any, valueLoader: Callable<T>): T? {
        var loaderCalled = false
        val wrappedLoader = Callable {
            loaderCalled = true
            valueLoader.call()
        }
        val result = delegate.get(key, wrappedLoader)
        if (loaderCalled) {
            metricsService.recordCacheMiss()
        } else {
            metricsService.recordCacheHit()
        }
        return result
    }

    override fun put(key: Any, value: Any?) {
        delegate.put(key, value)
    }

    override fun putIfAbsent(key: Any, value: Any?): Cache.ValueWrapper? {
        return delegate.putIfAbsent(key, value)
    }

    override fun evict(key: Any) {
        delegate.evict(key)
    }

    override fun evictIfPresent(key: Any): Boolean {
        return delegate.evictIfPresent(key)
    }

    override fun clear() {
        delegate.clear()
    }

    override fun invalidate(): Boolean {
        return delegate.invalidate()
    }
}
