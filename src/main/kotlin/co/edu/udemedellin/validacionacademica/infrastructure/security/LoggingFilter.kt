package co.edu.udemedellin.validacionacademica.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Filtro de logging centralizado.
 *
 * Para cada solicitud:
 * - Genera (o propaga) un X-Request-ID y lo expone en la respuesta.
 * - Registra en MDC: requestId, method, uri — disponible en todos los logs del hilo.
 * - Loguea la entrada (método + URI) y la salida (status + duración en ms).
 * - Limpia el MDC al finalizar para evitar contaminación entre hilos reutilizados.
 */
@Component
@Order(2)
class LoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestId = request.getHeader("X-Request-ID")
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString().replace("-", "").take(12)

        MDC.put("requestId", requestId)
        response.addHeader("X-Request-ID", requestId)

        val start = System.currentTimeMillis()
        try {
            log.info("--> {} {}", request.method, request.requestURI)
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - start
            log.info("<-- {} {} {} ({}ms)", request.method, request.requestURI, response.status, duration)
            MDC.clear()
        }
    }
}
