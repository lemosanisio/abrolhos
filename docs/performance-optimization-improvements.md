# Performance Optimization Improvements for Abrolhos

## Overview
This document outlines performance optimization strategies for the Abrolhos application, focusing on caching, database indexing, query optimization, and response compression to improve scalability and user experience.

## Current Performance Issues

### 1. No Caching Layer
**Location**: All service layers and controllers

**Issue**: Every request hits the database, even for frequently accessed, rarely changing data (categories, tags, published posts).

**Impact**:
- High database load
- Slow response times for repeated queries
- Poor scalability under load
- Unnecessary database connections

### 2. Missing Database Indexes
**Location**: `Backend/abrolhos/src/main/resources/db/migration/`

**Issue**: No indexes on frequently queried columns (slug, username, category_id, tag_id, status).

**Impact**:
- Slow queries on large datasets
- Full table scans for lookups
- Poor performance as data grows
- High CPU usage on database

### 3. N+1 Query Problems
**Location**: Post queries with categories and tags

**Issue**: Lazy loading causes multiple queries when fetching posts with relationships.

**Impact**:
- Hundreds of queries for paginated results
- Slow API response times
- Database connection pool exhaustion
- Poor user experience

### 4. No Response Compression
**Location**: API responses

**Issue**: Large JSON responses sent uncompressed over network.

**Impact**:
- High bandwidth usage
- Slow response times on slow connections
- Increased hosting costs
- Poor mobile experience

---

## Proposed Solutions

### Solution 1: Implement Multi-Layer Caching Strategy

#### Requirements
```xml
<requirement id="PERF-001" priority="high">
  <title>Implement Redis-Based Caching</title>
  <description>
    Add Redis caching for frequently accessed, rarely changing data with appropriate TTL and invalidation strategies
  </description>
  <acceptance-criteria>
    - Redis cache configured with Spring Cache abstraction
    - Categories cached with 1 hour TTL
    - Tags cached with 1 hour TTL
    - Published posts cached with 5 minute TTL
    - Post by slug cached with 5 minute TTL
    - Cache invalidation on create/update/delete operations
    - Cache hit rate &gt; 80% for cached endpoints
    - Cache miss penalty &lt; 10ms
    - Graceful degradation if Redis unavailable
  </acceptance-criteria>
</requirement>
```

#### Caching Strategy

**What to Cache**:
1. **Categories** (rarely change)
   - Cache key: `categories:all`
   - TTL: 1 hour
   - Invalidate on: category create/update/delete

2. **Tags** (rarely change)
   - Cache key: `tags:all`
   - TTL: 1 hour
   - Invalidate on: tag create/update/delete

3. **Published Posts List** (changes frequently)
   - Cache key: `posts:published:page:{page}:size:{size}`
   - TTL: 5 minutes
   - Invalidate on: post publish/unpublish

4. **Post by Slug** (changes occasionally)
   - Cache key: `post:slug:{slug}`
   - TTL: 5 minutes
   - Invalidate on: post update

5. **Post Count by Status** (for pagination)
   - Cache key: `posts:count:status:{status}`
   - TTL: 5 minutes
   - Invalidate on: post status change

**What NOT to Cache**:
- Draft posts (user-specific, change frequently)
- Authentication tokens (security risk)
- User sessions (already in JWT)
- Audit logs (must be real-time)

#### Implementation Details

```kotlin
// Cache Configuration
@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val cacheConfigurations = mapOf(
            "categories" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)),
            "tags" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)),
            "posts" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)),
            "postBySlug" to RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
        )
        
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}

// Service Layer Caching
@Service
class PostService(
    private val postRepository: PostRepository,
    private val cacheManager: CacheManager
) {
    @Cacheable(value = ["posts"], key = "#status + ':' + #page + ':' + #size")
    fun getPostsByStatus(status: PostStatus, page: Int, size: Int): Page<PostSummary> {
        return postRepository.findByStatus(status, PageRequest.of(page, size))
    }
    
    @Cacheable(value = ["postBySlug"], key = "#slug")
    fun getPostBySlug(slug: PostSlug): Post? {
        return postRepository.findBySlug(slug)
    }
    
    @CacheEvict(value = ["posts", "postBySlug"], allEntries = true)
    fun createPost(post: Post): Post {
        return postRepository.save(post)
    }
}
```

#### Testing Strategy
- Unit tests for cache configuration
- Integration tests verifying cache hits/misses
- Property-based tests for cache key generation
- Load tests measuring cache performance impact
- Tests for cache invalidation scenarios
- Tests for Redis failure scenarios

---

### Solution 2: Add Database Indexes

#### Requirements
```xml
<requirement id="PERF-002" priority="critical">
  <title>Create Database Indexes for Query Optimization</title>
  <description>
    Add indexes on frequently queried columns to improve query performance
  </description>
  <acceptance-criteria>
    - Index on posts.slug (unique)
    - Index on posts.status
    - Index on posts.published_at
    - Index on users.username (unique)
    - Composite index on posts (status, published_at) for published post queries
    - Index on post_tags.post_id and post_tags.tag_id
    - Index on post_categories.post_id and post_categories.category_id
    - Query performance improved by &gt; 90% for indexed queries
    - Index size &lt; 20% of table size
    - No negative impact on write performance
  </acceptance-criteria>
</requirement>
```

#### Index Strategy

**Single Column Indexes**:
```sql
-- Posts table
CREATE INDEX idx_posts_slug ON posts(slug);
CREATE INDEX idx_posts_status ON posts(status);
CREATE INDEX idx_posts_published_at ON posts(published_at);
CREATE INDEX idx_posts_created_at ON posts(created_at);

-- Users table
CREATE UNIQUE INDEX idx_users_username ON users(username);

-- Categories table
CREATE UNIQUE INDEX idx_categories_slug ON categories(slug);

-- Tags table
CREATE UNIQUE INDEX idx_tags_slug ON tags(slug);
```

**Composite Indexes** (for common query patterns):
```sql
-- Published posts ordered by date
CREATE INDEX idx_posts_status_published_at ON posts(status, published_at DESC);

-- Posts by category and status
CREATE INDEX idx_post_categories_category_status ON post_categories(category_id, post_id);

-- Posts by tag and status
CREATE INDEX idx_post_tags_tag_post ON post_tags(tag_id, post_id);

-- Invites by token and expiry
CREATE INDEX idx_invites_token_expires ON invites(token, expires_at);
```

**Full-Text Search Indexes** (for future search feature):
```sql
-- Full-text search on post title and content
CREATE INDEX idx_posts_fulltext ON posts USING GIN(to_tsvector('english', title || ' ' || content));
```

#### Implementation Details

Create Flyway migration: `V3__add_performance_indexes.sql`

```sql
-- Add indexes for query optimization
-- Posts indexes
CREATE INDEX IF NOT EXISTS idx_posts_slug ON posts(slug);
CREATE INDEX IF NOT EXISTS idx_posts_status ON posts(status);
CREATE INDEX IF NOT EXISTS idx_posts_published_at ON posts(published_at);
CREATE INDEX IF NOT EXISTS idx_posts_status_published_at ON posts(status, published_at DESC);

-- Users indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Categories indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);

-- Tags indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_tags_slug ON tags(slug);

-- Junction table indexes
CREATE INDEX IF NOT EXISTS idx_post_categories_category_id ON post_categories(category_id);
CREATE INDEX IF NOT EXISTS idx_post_categories_post_id ON post_categories(post_id);
CREATE INDEX IF NOT EXISTS idx_post_tags_tag_id ON post_tags(tag_id);
CREATE INDEX IF NOT EXISTS idx_post_tags_post_id ON post_tags(post_id);

-- Invites indexes
CREATE INDEX IF NOT EXISTS idx_invites_token ON invites(token);
CREATE INDEX IF NOT EXISTS idx_invites_expires_at ON invites(expires_at);

-- Analyze tables to update statistics
ANALYZE posts;
ANALYZE users;
ANALYZE categories;
ANALYZE tags;
ANALYZE post_categories;
ANALYZE post_tags;
ANALYZE invites;
```

#### Index Maintenance
- Monitor index usage with `pg_stat_user_indexes`
- Remove unused indexes quarterly
- Rebuild fragmented indexes monthly
- Update table statistics after bulk operations

#### Testing Strategy
- Query performance tests before/after indexes
- Explain plan analysis for common queries
- Load tests with large datasets
- Write performance tests (ensure no degradation)
- Index size monitoring tests

---

### Solution 3: Optimize N+1 Queries with Eager Loading

#### Requirements
```xml
<requirement id="PERF-003" priority="high">
  <title>Eliminate N+1 Query Problems</title>
  <description>
    Use eager loading and join fetching to eliminate N+1 query problems when loading posts with relationships
  </description>
  <acceptance-criteria>
    - Post queries with categories use single JOIN query
    - Post queries with tags use single JOIN query
    - Post list queries load all relationships in 3 queries maximum (posts, categories, tags)
    - No lazy loading exceptions in production
    - Query count reduced by &gt; 90% for post list endpoints
    - Response time improved by &gt; 80% for post list endpoints
  </acceptance-criteria>
</requirement>
```

#### N+1 Query Problems Identified

**Problem 1: Post List with Categories**
```kotlin
// Current: N+1 queries (1 for posts + N for categories)
val posts = postRepository.findAll()
posts.forEach { post ->
    println(post.categories) // Triggers separate query per post
}
```

**Problem 2: Post List with Tags**
```kotlin
// Current: N+1 queries (1 for posts + N for tags)
val posts = postRepository.findAll()
posts.forEach { post ->
    println(post.tags) // Triggers separate query per post
}
```

#### Solutions

**Solution 1: Entity Graph**
```kotlin
@EntityGraph(attributePaths = ["categories", "tags"])
@Query("SELECT p FROM PostEntity p WHERE p.status = :status")
fun findByStatusWithRelations(status: PostStatus): List<PostEntity>
```

**Solution 2: JOIN FETCH**
```kotlin
@Query("""
    SELECT DISTINCT p FROM PostEntity p
    LEFT JOIN FETCH p.categories
    LEFT JOIN FETCH p.tags
    WHERE p.status = :status
    ORDER BY p.publishedAt DESC
""")
fun findByStatusEagerLoad(status: PostStatus): List<PostEntity>
```

**Solution 3: Batch Fetching**
```kotlin
@Entity
@BatchSize(size = 10)
class PostEntity {
    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    var categories: Set<CategoryEntity> = emptySet()
}
```

#### Implementation Details

Update repository methods:
```kotlin
interface PostRepository : JpaRepository<PostEntity, UUID> {
    
    @EntityGraph(attributePaths = ["categories", "tags", "author"])
    @Query("""
        SELECT p FROM PostEntity p
        WHERE p.status = :status
        AND p.deletedAt IS NULL
        ORDER BY p.publishedAt DESC
    """)
    fun findPublishedPostsWithRelations(
        status: PostStatus,
        pageable: Pageable
    ): Page<PostEntity>
    
    @EntityGraph(attributePaths = ["categories", "tags", "author"])
    fun findBySlugAndDeletedAtIsNull(slug: String): PostEntity?
}
```

Enable query logging to verify:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
```

#### Testing Strategy
- Query count tests (assert max 3 queries)
- Integration tests with query logging
- Performance tests comparing before/after
- Load tests with large datasets
- Tests for pagination with eager loading

---

### Solution 4: Enable Response Compression

#### Requirements
```xml
<requirement id="PERF-004" priority="medium">
  <title>Enable GZIP Compression for API Responses</title>
  <description>
    Enable GZIP compression for API responses to reduce bandwidth and improve response times
  </description>
  <acceptance-criteria>
    - GZIP compression enabled for responses &gt; 1KB
    - Compression level set to 6 (balance speed/size)
    - JSON responses compressed by &gt; 70%
    - Compression overhead &lt; 5ms per request
    - Accept-Encoding header respected
    - Content-Encoding header set correctly
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**Spring Boot Configuration**:
```yaml
server:
  compression:
    enabled: true
    mime-types:
      - application/json
      - application/xml
      - text/html
      - text/xml
      - text/plain
      - application/javascript
      - text/css
    min-response-size: 1024  # 1KB
    level: 6  # Compression level (1-9, 6 is default)
```

**Nginx Configuration** (if using reverse proxy):
```nginx
gzip on;
gzip_vary on;
gzip_proxied any;
gzip_comp_level 6;
gzip_types
    text/plain
    text/css
    text/xml
    text/javascript
    application/json
    application/javascript
    application/xml+rss
    application/rss+xml
    application/atom+xml
    image/svg+xml;
gzip_min_length 1024;
```

#### Testing Strategy
- Integration tests verifying compression headers
- Response size tests (before/after compression)
- Performance tests measuring compression overhead
- Tests with different Accept-Encoding headers
- Load tests with compression enabled

---

## Additional Performance Optimizations

### 5. Connection Pool Tuning

#### Requirements
```xml
<requirement id="PERF-005" priority="medium">
  <title>Optimize Database Connection Pool</title>
  <description>
    Configure HikariCP connection pool for optimal performance based on workload
  </description>
  <acceptance-criteria>
    - Connection pool sized appropriately (formula: cores * 2 + effective_spindle_count)
    - Connection timeout set to 30 seconds
    - Idle timeout set to 10 minutes
    - Max lifetime set to 30 minutes
    - Connection leak detection enabled
    - Pool metrics exposed via actuator
  </acceptance-criteria>
</requirement>
```

#### Configuration
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10  # Adjust based on load
      minimum-idle: 5
      connection-timeout: 30000  # 30 seconds
      idle-timeout: 600000  # 10 minutes
      max-lifetime: 1800000  # 30 minutes
      leak-detection-threshold: 60000  # 1 minute
      pool-name: AbrolhosHikariPool
```

---

### 6. Pagination Optimization

#### Requirements
```xml
<requirement id="PERF-006" priority="medium">
  <title>Implement Cursor-Based Pagination</title>
  <description>
    Replace offset-based pagination with cursor-based pagination for better performance on large datasets
  </description>
  <acceptance-criteria>
    - Cursor-based pagination for post lists
    - Cursor encodes last item ID and timestamp
    - Consistent results even with concurrent updates
    - Performance independent of page number
    - Backward pagination supported
    - Fallback to offset pagination for compatibility
  </acceptance-criteria>
</requirement>
```

#### Implementation
```kotlin
data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: String?,
    val prevCursor: String?,
    val hasNext: Boolean,
    val hasPrev: Boolean
)

@Query("""
    SELECT p FROM PostEntity p
    WHERE p.status = :status
    AND (p.publishedAt < :cursorDate OR (p.publishedAt = :cursorDate AND p.id < :cursorId))
    ORDER BY p.publishedAt DESC, p.id DESC
    LIMIT :limit
""")
fun findByStatusAfterCursor(
    status: PostStatus,
    cursorDate: Instant,
    cursorId: UUID,
    limit: Int
): List<PostEntity>
```

---

## Performance Testing Strategy

### Load Testing Scenarios

**Scenario 1: Homepage Load**
- 100 concurrent users
- Fetch published posts (page 1)
- Target: < 200ms p95 response time

**Scenario 2: Post Detail View**
- 50 concurrent users
- Fetch random post by slug
- Target: < 150ms p95 response time

**Scenario 3: Category Browse**
- 30 concurrent users
- Fetch posts by category
- Target: < 250ms p95 response time

**Scenario 4: Search**
- 20 concurrent users
- Full-text search queries
- Target: < 500ms p95 response time

### Performance Benchmarks

**Before Optimization**:
- Post list query: ~500ms (N+1 queries)
- Post detail query: ~200ms (lazy loading)
- Category list: ~100ms (no cache)
- Database connections: 50-80% utilization

**After Optimization**:
- Post list query: < 50ms (eager loading + cache)
- Post detail query: < 20ms (cache hit)
- Category list: < 5ms (cache hit)
- Database connections: 20-30% utilization

### Monitoring Metrics

**Application Metrics**:
- Request rate (requests/second)
- Response time (p50, p95, p99)
- Error rate (%)
- Cache hit rate (%)
- Database query time (ms)

**Database Metrics**:
- Active connections
- Query execution time
- Index hit rate
- Table scan rate
- Lock wait time

**Infrastructure Metrics**:
- CPU utilization (%)
- Memory usage (MB)
- Network I/O (MB/s)
- Disk I/O (IOPS)

---

## Implementation Roadmap

### Phase 1: Quick Wins (Week 1)
1. Enable response compression (1 day)
2. Add database indexes (2 days)
3. Configure connection pool (1 day)

### Phase 2: Caching (Week 2)
1. Set up Redis infrastructure (1 day)
2. Implement cache configuration (2 days)
3. Add cache invalidation logic (2 days)

### Phase 3: Query Optimization (Week 3)
1. Fix N+1 queries with eager loading (3 days)
2. Implement cursor-based pagination (2 days)

### Phase 4: Testing & Monitoring (Week 4)
1. Load testing and benchmarking (2 days)
2. Set up performance monitoring (2 days)
3. Documentation and training (1 day)

---

## Success Metrics

### Performance Targets
- API response time reduced by 80%
- Database query count reduced by 90%
- Cache hit rate > 80%
- Page load time < 1 second
- Time to first byte < 100ms

### Scalability Targets
- Support 1000 concurrent users
- Handle 10,000 requests/minute
- Database connections < 30% capacity
- CPU utilization < 60% under load
- Memory usage < 70% capacity

### Cost Savings
- Reduce database instance size by 50%
- Reduce bandwidth costs by 70%
- Reduce infrastructure costs by 40%

---

## Dependencies

```kotlin
// Redis Cache
implementation("org.springframework.boot:spring-boot-starter-data-redis")
implementation("org.springframework.boot:spring-boot-starter-cache")

// Connection Pool (already included)
implementation("com.zaxxer:HikariCP")

// Monitoring
implementation("org.springframework.boot:spring-boot-starter-actuator")
implementation("io.micrometer:micrometer-registry-prometheus")
```

---

## Rollback Plan

### If Performance Degrades
1. **Caching**: Disable cache, revert to direct database queries
2. **Indexes**: Drop problematic indexes (rare, but possible)
3. **Eager Loading**: Revert to lazy loading with batch fetching
4. **Compression**: Disable compression if CPU spikes

### Monitoring Triggers
- Response time increase > 20%
- Error rate increase > 2%
- Cache hit rate < 50%
- Database connection pool exhaustion
