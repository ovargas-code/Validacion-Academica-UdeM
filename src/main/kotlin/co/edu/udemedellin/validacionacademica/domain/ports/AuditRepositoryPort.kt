package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.AuditAction
import co.edu.udemedellin.validacionacademica.domain.model.AuditEvent

/**
 * Puerto de salida para la persistencia del registro de auditoría.
 *
 * Los eventos de auditoría son inmutables una vez guardados. El guardado se realiza
 * en una transacción independiente ([Propagation.REQUIRES_NEW]) para garantizar que
 * queden registrados aunque la operación principal haga rollback.
 */
interface AuditRepositoryPort {

    /**
     * Persiste un evento de auditoría y retorna la instancia guardada con su ID asignado.
     */
    fun save(event: AuditEvent): AuditEvent

    /**
     * Retorna los eventos de auditoría más recientes, opcionalmente filtrados.
     *
     * @param performedBy nombre del administrador que ejecutó la acción; `null` para no filtrar por usuario.
     * @param action tipo de acción auditada; `null` para incluir todos los tipos.
     * @param limit número máximo de eventos a retornar, ordenados por fecha descendente.
     * @return lista de eventos que coinciden con los filtros, vacía si no hay resultados.
     */
    fun findRecent(performedBy: String?, action: AuditAction?, limit: Int): List<AuditEvent>
}
