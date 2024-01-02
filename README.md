# Intro
ratelimit4j is a Java library that implements different rate limiting algorithms.
The algorithms currently implemented are:
- Token Bucket
- Leaky Bucket
- Fixed Window Counter

# Project structure
The library is structured as a multimodule Maven project and is composed as follows:
- **ratelimit4j-core**: contains the base classes and interfaces for implementing the different rate limiting algorithms. It is used as a dependency for the concrete implementations, which are currently: Caffeine, Redis.
- **ratelimit4j-caffeine**: implements the different rate limiting algorithms using the Caffeine cache. Depends on ratelimit4j-core.
- **ratelimit4j-redis**: implements the different rate limiting algorithms using the Redis cache. Depends on ratelimit4j-core.
- **ratelimit4j-core-spring-boot**: contains the base classes and interfaces used by the spring boot starters. Depends on ratelimit4j-core.
- **ratelimit4j-caffeine-spring-boot-starter**: spring boot starter that implements algorithms using spring boot autoconfiguration. It allows rate limiters to be used programmatically as in the pure Java library (ratelimit4-caffeine), but also allows rate limiters to be configured on http request paths within spring properties. The algorithms are implemented using the Caffeine cache. It depends on ratelimit4j-core-spring-boot and ratelimit4j-caffeine.
- **ratelimit4j-redis-spring-boot-starter**: spring boot starter that implements the algorithms using spring boot autoconfiguration. It allows rate limiters to be used programmatically as in the pure Java library (ratelimit4-redis), but also to configure the rate limiter on http request paths within spring properties. The algorithms are implemented using the Redis cache. It depends on ratelimit4j-core-spring-boot and ratelimit4j-redis.

# Usage

## Pure Java Caffeine
1) The project is not currently on Maven Central, so you will need to download this repository and run the clean install locally (Maven version tested 3.8.1):
```
mvn clean install
```
3) Place the Maven dependency inside your project:
```
<dependency>
    <groupId>com.github.scarrozzo</groupId>
    <artifactId>ratelimit4j-caffeine</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```
4) Instantiate the rate limiter with one of the algorithms you intend to use:
   
   4.1) Token Bucket example:
   ```
    RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new CaffeineTokenBucketRateLimiter(
        new TokenBucketRateLimiterConfig(2L, 2_000L));
   ```
   4.2) Leaky Bucket example:
   ```
    RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new CaffeineLeakyBucketRateLimiter(
       new LeakyBucketRateLimiterConfig(1L, 1L, 10L, 1000L));
    ```
   4.3) Fixed Window Counter example:
   ```
    RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new CaffeineFixedWindowCounterRateLimiter(
       new FixedWindowCounterRateLimiterConfig(500L, 1L));
   ```
5) Invoke the rate limiter's "evalutateRequest" method where you want to apply the rate limiter (the name of the method is indifferent from the algorithm used/instantiated):
    ```
     rateLimiter.evaluateRequest(key);
   ```

## Pure Java Redis
1) The project is not currently on Maven Central, so you will need to download this repository and run the clean install locally (Maven version tested 3.8.1):
```
mvn clean install
```
3) Place the Maven dependency inside your project:
```
<dependency>
    <groupId>com.github.scarrozzo</groupId>
    <artifactId>ratelimit4j-redis</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```
4) The Redis implementation of the algorithms is based on the "Redisson" client, so compared to the Caffeine variant it will be necessary to pass an already instantiated Redisson client and TransactionOptions. Instantiate the rate limiter with one of the algorithms you intend to use:
   
   4.1) Token Bucket example:
   ```
   RedissonClient redissonClient = Redisson.create(Config.fromYAML("""
                        singleServerConfig:
                            address: "redis://127.0.0.1:6379"
                        """));
    RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new RedisTokenBucketRateLimiter(
       new TokenBucketRateLimiterConfig(1L, 2000L), redissonClient, TransactionOptions.defaults());
   ```
   4.2) Leaky Bucket example:
   ```
    RedissonClient redissonClient = Redisson.create(Config.fromYAML("""
                        singleServerConfig:
                            address: "redis://127.0.0.1:6379"
                        """));
    RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new RedisLeakyBucketRateLimiter(
       new LeakyBucketRateLimiterConfig(1L, 1L, 10L, 1000L), redissonClient, TransactionOptions.defaults());
    ```
   4.3) Fixed Window Counter example:
   ```
    RedissonClient redissonClient = Redisson.create(Config.fromYAML("""
                        singleServerConfig:
                            address: "redis://127.0.0.1:6379"
                        """));
    RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new RedisFixedWindowCounterRateLimiter(
       new FixedWindowCounterRateLimiterConfig(500L, 1L), redissonClient, TransactionOptions.defaults());
   ```
5) Invoke the rate limiter's "evalutateRequest" method where you want to apply the rate limiter (the name of the method is indifferent from the algorithm used/instantiated):
    ```
     rateLimiter.evaluateRequest(key);
   ```

## Spring boot Caffeine
TODO

## Spring boot Redis
TODO
