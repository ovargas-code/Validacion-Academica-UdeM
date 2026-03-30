package co.edu.udemedellin.validacionacademica.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.rate-limit")
data class RateLimitProperties(
    val cache: CacheConfig = CacheConfig(),
    val rules: List<RuleConfig> = emptyList()
) {
    data class CacheConfig(
        val expireAfterAccessMinutes: Long = 10,
        val maximumSize: Long = 100_000
    )

    data class RuleConfig(
        val prefix: String,
        val capacity: Long,
        val refillMinutes: Long = 1
    ) {
        fun refillDuration(): Duration = Duration.ofMinutes(refillMinutes)
    }
}
