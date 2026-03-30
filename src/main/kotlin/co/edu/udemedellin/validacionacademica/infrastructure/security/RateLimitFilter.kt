package co.edu.udemedellin.validacionacademica.infrastructure.security

import co.edu.udemedellin.validacionacademica.infrastructure.config.RateLimitProperties
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.TimeUnit

/**
 * Rate limiting por IP para endpoints sensibles.
 * Los límites se configuran en application.yml bajo app.rate-limit.rules.
 *
 * Usa Caffeine como caché con expiración automática para evitar memory leaks
 * ante volumen alto de IPs únicas.
 */
@Component
@Order(1)
class RateLimitFilter(private val rateLimitProperties: RateLimitProperties) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RateLimitFilter::class.java)

    private val buckets by lazy {
        Caffeine.newBuilder()
            .expireAfterAccess(rateLimitProperties.cache.expireAfterAccessMinutes, TimeUnit.MINUTES)
            .maximumSize(rateLimitProperties.cache.maximumSize)
            .build<String, Bucket>()
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val uri = request.requestURI
        val matched = rateLimitProperties.rules.firstOrNull { rule -> uri.startsWith(rule.prefix) }

        if (matched == null) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = resolveClientIp(request)
        val bucket = buckets.get("${matched.prefix}|$clientIp") { createBucket(matched) }
            ?: throw IllegalStateException("No se pudo obtener o crear el bucket de rate-limiting para $clientIp")

        val probe = bucket.tryConsumeAndReturnRemaining(1)

        if (probe.isConsumed) {
            response.addHeader("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
            filterChain.doFilter(request, response)
        } else {
            val waitSeconds = probe.nanosToWaitForRefill / 1_000_000_000
            log.warn("Rate limit excedido para IP: {} en {}", clientIp, uri)
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", waitSeconds.toString())
            response.writer.write(
                """{"status":429,"error":"Too Many Requests","message":"Límite de solicitudes excedido. Intente nuevamente en ${waitSeconds}s."}"""
            )
        }
    }

    private fun createBucket(rule: RateLimitProperties.RuleConfig): Bucket = Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(rule.capacity)
                .refillGreedy(rule.capacity, rule.refillDuration())
                .build()
        )
        .build()

    /**
     * Resuelve la IP real del cliente.
     * Solo confía en X-Forwarded-For si la petición viene de un proxy interno
     * (loopback o rango privado: nginx en Docker, load balancer, etc.).
     */
    private fun resolveClientIp(request: HttpServletRequest): String {
        val remoteAddr = request.remoteAddr
        return if (isFromTrustedProxy(remoteAddr)) {
            request.getHeader("X-Forwarded-For")
                ?.split(",")?.first()?.trim()
                ?.takeIf { it.isNotBlank() } ?: remoteAddr
        } else {
            remoteAddr
        }
    }

    private fun isFromTrustedProxy(remoteAddr: String): Boolean {
        return remoteAddr == "127.0.0.1" ||
               remoteAddr == "::1" ||
               remoteAddr.startsWith("10.") ||
               remoteAddr.startsWith("192.168.") ||
               remoteAddr.startsWith("172.")
    }
}
