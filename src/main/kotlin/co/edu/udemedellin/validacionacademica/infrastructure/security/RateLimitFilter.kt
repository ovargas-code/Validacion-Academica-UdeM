package co.edu.udemedellin.validacionacademica.infrastructure.security

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
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Rate limiting por IP para endpoints sensibles.
 *
 * Límites configurados:
 *  - /api/auth/login          → 5 req/min  (protección brute-force)
 *  - /api/validations/        → 10 req/min (endpoint público de validación)
 *  - /api/v1/verificaciones/  → 30 req/min (verificación de certificados)
 *
 * Usa Caffeine como caché con expiración automática para evitar memory leaks
 * ante volumen alto de IPs únicas.
 */
@Component
@Order(1)
class RateLimitFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RateLimitFilter::class.java)

    private data class LimitConfig(val capacity: Long, val refillDuration: Duration)

    private val limits = listOf(
        "/api/auth/login"           to LimitConfig(5,  Duration.ofMinutes(1)),
        "/api/validations/confirm"  to LimitConfig(5,  Duration.ofMinutes(1)),
        "/api/validations/"         to LimitConfig(10, Duration.ofMinutes(1)),
        "/api/v1/verificaciones/"   to LimitConfig(30, Duration.ofMinutes(1))
    )

    // Cache con evición automática: previene memory leak por IPs únicas
    private val buckets = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .maximumSize(100_000)
        .build<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val uri = request.requestURI
        val matched = limits.firstOrNull { (prefix, _) -> uri.startsWith(prefix) }

        if (matched == null) {
            filterChain.doFilter(request, response)
            return
        }

        val (prefix, config) = matched
        val clientIp = resolveClientIp(request)
        val bucket = buckets.get("$prefix|$clientIp") { createBucket(config) }!!

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

    private fun createBucket(config: LimitConfig): Bucket = Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(config.capacity)
                .refillGreedy(config.capacity, config.refillDuration)
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
