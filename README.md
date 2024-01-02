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

Coming soon
