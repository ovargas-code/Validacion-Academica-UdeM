package co.edu.udemedellin.validacionacademica.domain.ports

import co.edu.udemedellin.validacionacademica.domain.model.Student
import co.edu.udemedellin.validacionacademica.domain.model.StudentPage

/**
 * Puerto de salida para la persistencia de [Student].
 *
 * Las implementaciones deben garantizar que el campo [Student.document] sea único
 * dentro del almacén de datos. Cualquier violación de unicidad debe propagarse como
 * una excepción no verificada para que la capa de aplicación pueda manejarla.
 */
interface StudentRepositoryPort {

    /**
     * Persiste un nuevo estudiante y retorna la instancia guardada con su ID asignado.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException si ya existe un estudiante con el mismo [Student.document].
     */
    fun save(student: Student): Student

    /**
     * Busca un estudiante por número de documento.
     *
     * @return el estudiante encontrado, o `null` si no existe ningún registro con ese documento.
     */
    fun findByDocument(document: String): Student?

    /**
     * Retorna una página de estudiantes ordenada por criterio de la implementación.
     *
     * @param page índice de página basado en cero.
     * @param size número máximo de elementos por página.
     */
    fun findAll(page: Int, size: Int): StudentPage

    /**
     * Actualiza los datos del estudiante identificado por [document].
     *
     * @return el estudiante actualizado, o `null` si no existe ningún registro con ese documento.
     */
    fun updateByDocument(document: String, student: Student): Student?

    /**
     * Elimina el estudiante identificado por [document].
     *
     * @return `true` si el estudiante existía y fue eliminado, `false` si no se encontró ningún registro.
     */
    fun deleteByDocument(document: String): Boolean
}
